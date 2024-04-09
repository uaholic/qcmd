package com.guanyanqi;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.utils.QCmdUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QCmd类是命令行处理工具的核心类，负责解析命令行参数，并根据参数注解将解析结果映射到相应的字段上。
 * 它支持基本数据类型、集合、映射类型的自动转换，还可以处理自定义类型的转换逻辑。
 * 此外，它还能根据注解自动生成命令行工具的帮助信息。
 *
 * @author guanyanqi
 */
public class QCmd {

    private Object value; // 解析结果映射到的目标对象
    private String[] args; // 命令行参数
    private String desc; // 自动生成的帮助信息
    private final Map<String, String> argsParamValueMap = new HashMap<>(); // 参数名到参数值的映射
    private final Map<String, Field> clazzParamFieldMap = new HashMap<>(); // 参数名到类字段的映射
    private final Map<Field, String> fieldValueMap = new HashMap<>(); // 类字段到参数值的映射
    private final Set<String> boolParams = new HashSet<>(); // 存储布尔类型参数名
    private Set<String> cmdNames; // 命令名集合
    private final List<String> vars = new ArrayList<>(); // 存储@Vars注解标记的变量参数
    private Field varField; // 存储变量参数的字段
    private final List<List<String>> requiredParamLists = new ArrayList<>(); // 存储必须至少有一个参数被指定的参数组

    /**
     * 静态工厂方法，用于创建QCmd实例。
     *
     * @param args 命令行参数
     * @return QCmd实例
     */
    public static QCmd of(String[] args) {
        return new QCmd().args(args);
    }

    /**
     * 设置命令行参数并返回当前实例。
     *
     * @param args 命令行参数
     * @return 当前QCmd实例
     */
    private QCmd args(String[] args) {
        this.args = args;
        return this;
    }

    /**
     * 解析命令行参数，并将解析结果映射到指定类的实例上。
     *
     * @param <T>   目标类的类型
     * @param clazz 目标类
     * @return 映射了命令行参数的类实例
     */
    public <T> T parse(Class<T> clazz) {
        this.value = QCmdUtils.buildInstance(clazz);
        parse(args);
        return (T) value;
    }

    /**
     * 解析命令行参数的主要逻辑。
     *
     * @param args 命令行参数
     */
    public void parse(String... args) {
        try {
            qCmdInit();
            parseValues(args);
            validateAndMapping();
            buildInstance();
        } catch (Exception e) {
            throw new QCmdException("格式化参数异常.", e, this);
        }
    }

    /**
     * 根据已解析的参数值构建目标对象的实例，将参数值映射到对象的字段上。
     */
    private void buildInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (Map.Entry<Field, String> entry : fieldValueMap.entrySet()) {
            Field field = entry.getKey();
            String v = entry.getValue();
            field.setAccessible(true);
            Parameter annotation = field.getAnnotation(Parameter.class);
            Object paramValue;
            Class<? extends QStringConverter<?>> customConverter = annotation.converter();
            if (NoConverter.class != customConverter) {
                Constructor<? extends QStringConverter<?>> constructor = customConverter.getDeclaredConstructor();
                constructor.setAccessible(true);
                QStringConverter<?> converter = constructor.newInstance();
                paramValue = converter.convert(v);
            } else {
                paramValue = QCmdUtils.convert2Type(field, field.getType(), v);
            }
            field.set(value, paramValue);
        }
        if (CollectionUtils.isNotEmpty(vars)) {
            varField.setAccessible(true);
            Vars annotation = varField.getAnnotation(Vars.class);
            Class<?> varType = varField.getType();
            Class<? extends QStringConverter<?>> customConverter = annotation.elementConverter();
            if (!Collection.class.isAssignableFrom(varType)) {
                if (vars.size() != 1) {
                    throw new QCmdException(cmdNames + " 命令不支持多个变量。", this);
                }
                String value = vars.get(0);
                if (NoConverter.class != customConverter) {
                    Constructor<? extends QStringConverter<?>> constructor = customConverter.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    QStringConverter<?> converter = constructor.newInstance();
                    varField.set(this.value, converter.convert(value));
                } else {
                    varField.set(this.value, QCmdUtils.convert2Type(varField.getType(), value));
                }
            } else {
                Collection collection = QCmdUtils.createCollectionByType(varType);
                if (NoConverter.class != customConverter) {
                    Constructor<? extends QStringConverter<?>> constructor = customConverter.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    QStringConverter<?> converter = constructor.newInstance();
                    for (String var : vars) {
                        collection.add(converter.convert(var));
                    }
                } else {
                    ParameterizedType pType = (ParameterizedType) varField.getGenericType();
                    Type[] actualTypeArguments = pType.getActualTypeArguments();
                    Class<?> elementType = (Class<?>) actualTypeArguments[0];
                    for (String var : vars) {
                        collection.add(elementType.cast(QCmdUtils.convert2Type(elementType, var)));
                    }
                }
                varField.set(this.value, collection);
            }
        }
    }

    /**
     * 解析命令行参数值，分离命令名、参数和变量。
     *
     * @param args 命令行参数
     */
    private void parseValues(String[] args) {
        if (args == null || args.length == 0) {
            throw new QCmdException("命令内容为空", this);
        }
        List<String> tmpArgs = Lists.newArrayList(args);
        int argsSize = tmpArgs.size();
        String cmd = tmpArgs.get(0);
        if (!cmdNames.contains(cmd)) {
            throw new QCmdException("执行的命令与参数接收对象不匹配");
        }
        for (int i = 1; i < argsSize; i++) {
            String currArg = tmpArgs.get(i);
            if (currArg.startsWith("-")) {
                if (i + 1 < argsSize) {
                    String nextArg = tmpArgs.get(i + 1);
                    if (!nextArg.startsWith("-")) {
                        argsParamValueMap.put(currArg, nextArg);
                        i++;
                        continue;
                    }
                }
                argsParamValueMap.put(currArg, boolParams.contains(currArg) ? "true" : "");
            } else {
                vars.add(currArg);
            }
        }
    }

    /**
     * 初始化QCmd实例，获取命令信息、解析并校验参数注解。
     */
    private void qCmdInit() {
        Class<?> valueClass = value.getClass();
        Cmd cmdAnnotation = valueClass.getAnnotation(Cmd.class);
        if (cmdAnnotation == null) {
            throw new QCmdException("命令类" + valueClass.getName() + "没有添加@Cmd注解");
        }
        if (cmdAnnotation.names().length == 0) {
            throw new QCmdException("命令类" + valueClass.getName() + "@Cmd注解没有声明names");
        }
        cmdNames = Sets.newHashSet(cmdAnnotation.names());
        List<Field> fields = FieldUtils.getAllFieldsList(valueClass);
        Vars vars = null;
        for (Field field : fields) {
            Parameter annotation = field.getAnnotation(Parameter.class);
            if (Objects.nonNull(annotation)) {
                for (String name : annotation.names()) {
                    Field oldField = clazzParamFieldMap.put(name, field);
                    if (Objects.nonNull(oldField)) {
                        throw new QCmdException("参数 " + name + " 被重复声明到多个不同的属性");
                    }
                    if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        boolParams.add(name);
                    }
                }
                if (annotation.required()) {
                    requiredParamLists.add(Lists.newArrayList(annotation.names()));
                }
            }
            Vars varsAnnotation = field.getAnnotation(Vars.class);
            if (Objects.nonNull(varsAnnotation)) {
                if (Objects.isNull(vars)) {
                    vars = varsAnnotation;
                    varField = field;
                } else {
                    throw new QCmdException("变量参数只能有一个");
                }
            }
        }
        this.desc = QCmdUtils.createDesc(cmdAnnotation, fields, vars);
    }

    /**
     * 验证命令行参数并将它们映射到相应的字段上。
     */
    private void validateAndMapping() {
        argsParamValueMap.forEach((arg, value) -> {
            Field field = clazzParamFieldMap.get(arg);
            validateParam(arg, value, field);
            fieldValueMap.put(field, value);
        });
        validateRequiredParams();
    }

    /**
     * 验证命令行参数是否有不支持的参数，以及参数值是否符合正则校验规则
     */
    private void validateParam(String arg, String value, Field field) {
        if (Objects.isNull(field)) {
            throw new QCmdException(cmdNames + " 命令不支持 " + arg + " 参数。", this);
        }
        Parameter annotation = field.getAnnotation(Parameter.class);
        String valueValidRegex = annotation.valueValidRegex();
        if (StringUtils.isNotBlank(valueValidRegex)) {
            Pattern pattern = Pattern.compile(valueValidRegex);
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                throw new QCmdException(cmdNames + " 命令 " + arg + " 参数 校验失败。输入规则：" + annotation.valueValidDesc(), this);
            }
        }
    }

    /**
     * 验证命令行必传参数是否被声明
     */
    private void validateRequiredParams() {
        requiredParamLists.stream()
                .filter(requiredParamList -> requiredParamList.stream().noneMatch(argsParamValueMap::containsKey))
                .findFirst()
                .ifPresent(missingParamList -> {
                    throw new QCmdException(cmdNames + " 命令 " + String.join("|", missingParamList) + " 参数必填。", this);
                });
        if (CollectionUtils.isNotEmpty(vars) && Objects.isNull(varField)) {
            throw new QCmdException(cmdNames + " 命令不支持接收变量。", this);
        }
    }

    public Object getValue() {
        return value;
    }

    public String[] getArgs() {
        return args;
    }

    /**
     * 获取描述信息。（整个命令的整体详细帮助信息）
     */
    public String getDesc() {
        return desc;
    }
}

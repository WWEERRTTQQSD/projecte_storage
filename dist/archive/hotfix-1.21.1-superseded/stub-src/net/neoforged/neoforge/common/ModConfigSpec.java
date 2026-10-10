package net.neoforged.neoforge.common;

/**
 * 编译期桩类（不属于模组运行代码，仅用于单类热修重编译）。
 * 成员集合与签名严格对照两处事实：
 * 1) r8 jar 里旧 EmcLinkConfig.class 的字节码方法引用（comment/define(Z)/defineInRange/build/ConfigValue.get）；
 * 2) NeoForge 21.1.244 官方源码 ModConfigSpec.java（define(List) L309、BooleanValue L1284、IntValue L1303）。
 */
public class ModConfigSpec {
    public static class ConfigValue<T> {
        public T get() { throw new UnsupportedOperationException("stub"); }
    }
    public static class BooleanValue extends ConfigValue<Boolean> {}
    public static class IntValue extends ConfigValue<Integer> {}
    public static class LongValue extends ConfigValue<Long> {}
    public static class DoubleValue extends ConfigValue<Double> {}
    public static class Builder {
        public Builder comment(String comment) { return this; }
        public Builder comment(String... comment) { return this; }
        public <T> ConfigValue<T> define(String path, T defaultValue) { return null; }
        public BooleanValue define(String path, boolean defaultValue) { return null; }
        public IntValue defineInRange(String path, int defaultValue, int min, int max) { return null; }
        public ModConfigSpec build() { return null; }
    }
}

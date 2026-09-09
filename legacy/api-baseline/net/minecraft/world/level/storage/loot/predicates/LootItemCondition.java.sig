public interface LootItemCondition extends LootContextUser, Predicate<LootContext> {
default LootItemCondition.Builder invert() {
default AnyOfCondition.Builder or(final LootItemCondition.Builder other) {
default AllOfCondition.Builder and(final LootItemCondition.Builder other) {

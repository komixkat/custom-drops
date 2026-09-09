public interface LootItemCondition extends LootContextUser, Predicate<LootContext>
default LootItemCondition invert()
default LootItemCondition or(LootItemCondition other)
default LootItemCondition and(LootItemCondition other)
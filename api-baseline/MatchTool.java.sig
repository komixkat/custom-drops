public record MatchTool(Optional<ItemPredicate> predicate) implements LootItemCondition
public static final MapCodec<MatchTool> MAP_CODEC
public MapCodec<MatchTool> codec()
public Set<ContextKey<?>> getReferencedContextParams()
public boolean test(LootContext context)
static boolean toolMatches(ItemInstance tool, ItemPredicate predicate)
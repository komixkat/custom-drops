public record MatchTool(Optional<ItemPredicate> predicate) implements LootItemCondition {
public static final MapCodec<MatchTool> MAP_CODEC = RecordCodecBuilder.mapCodec(
public MapCodec<MatchTool> codec() {
public Set<ContextKey<?>> getReferencedContextParams() {
public boolean test(final LootContext context) {
public static LootItemCondition.Builder toolMatches(final ItemPredicate.Builder predicate) {

public interface ItemInstance extends TypedInstance<Item>, DataComponentGetter {
default int getMaxStackSize() {

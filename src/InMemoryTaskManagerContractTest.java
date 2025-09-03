public class InMemoryTaskManagerContractTest extends TaskManagerBaseTest<InMemoryTaskManager> {
    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }
}
import org.jetbrains.kotlinx.lincheck.LinChecker;
import org.jetbrains.kotlinx.lincheck.LoggingLevel;
import org.jetbrains.kotlinx.lincheck.annotations.Operation;
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions;
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SetTest {
    private final Set<Integer> set = new SetImpl<>();

    @Operation
    public boolean add(Integer x) {
        return set.add(x);
    }

    @Operation
    public boolean remove(Integer x) {
        return set.remove(x);
    }

    @Operation
    public boolean contains(Integer x) {
        return set.contains(x);
    }

    @Operation
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Operation
    public List<Integer> iterator() {
        List<Integer> lst = new ArrayList<>();
        for (Iterator<Integer> it = set.iterator(); it.hasNext();){
            lst.add(it.next());
        }
        return lst;
    }

    @Test
    public void testSetModelCheckingSingleThread() {
        ModelCheckingOptions options = new ModelCheckingOptions()
                .actorsBefore(0)
                .threads(1)
                .actorsPerThread(10)
                .actorsAfter(0)
                .iterations(10)
                .invocationsPerIteration(1)
                .logLevel(LoggingLevel.INFO);

        LinChecker.check(SetTest.class, options);
    }


    @Test
    public void testSetModelCheckingMultipleThread() {
        ModelCheckingOptions options = new ModelCheckingOptions()
                .actorsBefore(0)
                .threads(10)
                .actorsPerThread(20)
                .actorsAfter(0)
                .iterations(10)
                .invocationsPerIteration(1)
                .logLevel(LoggingLevel.INFO);

        LinChecker.check(SetTest.class, options);
    }

    @Test
    public void testSetStressTest() {
        StressOptions options = new StressOptions()
                .logLevel(LoggingLevel.INFO);

        LinChecker.check(SetTest.class, options);
    }
}
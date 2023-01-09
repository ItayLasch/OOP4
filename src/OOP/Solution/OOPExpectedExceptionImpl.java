package OOP.Solution;

import java.util.ArrayList;

public class OOPExpectedExceptionImpl implements OOP.Provided.OOPExpectedException {

    private Class<? extends Exception> expectedExcept;
    private ArrayList<String> messages;

    private OOPExpectedExceptionImpl()
    {
        expectedExcept = null;
        messages = new ArrayList<>();
    }

    public Class<? extends Exception> getExpectedException() {
        return this.expectedExcept;
    }

    public OOPExpectedExceptionImpl expect(Class<? extends Exception> expected)
    {
        this.expectedExcept = expected;
        return this;
    }

    public OOPExpectedExceptionImpl expectMessage(String msg)
    {
        messages.add(msg);
        return this;
    }

    public boolean assertExpected(Exception e)
    {
        if (!this.expectedExcept.isInstance(e)) {
            return false;
        }

        return this.messages.stream().allMatch((msg) -> (e.getMessage().contains(msg)));
    }
    
    public static OOPExpectedExceptionImpl none()
    {
        return new OOPExpectedExceptionImpl();
    }
}

package OOP.Solution;

import java.util.ArrayList;

public class OOPExpectedExceptionImpl implements OOP.Provided.OOPExpectedException {

    private Class<? extends Exception> expectedExcep;
    private ArrayList<String> messages;

    private OOPExpectedExceptionImpl()
    {
        messages = new ArrayList<>();
    }

    public Class<? extends Exception> getExpectedException() {
        return this.expectedExcep;
    }

    public OOPExpectedExceptionImpl expect(Class<? extends Exception> expected)
    {
        this.expectedExcep = expected;
        return this;
    }

    public OOPExpectedExceptionImpl expectMessage(String msg)
    {
        messages.add(msg);
        return this;
    }

    public boolean assertExpected(Exception e)
    {
        if (!this.expectedExcep.isInstance(e)) {
            return false;
        }

        return this.messages.stream().anyMatch((msg) -> (!e.getMessage().contains(msg)));
    }
    
    public static OOPExpectedExceptionImpl none()
    {
        return new OOPExpectedExceptionImpl();
    }
}

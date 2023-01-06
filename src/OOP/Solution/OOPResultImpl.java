package OOP.Solution;

import OOP.Provided.*;

public class OOPResultImpl implements OOPResult {
    
    OOPTestResult result;
    String message;

    public OOPResultImpl(OOPTestResult res, String msg)
    {
        this.result = res;
        this.message = msg;
    }

    public OOPTestResult getResultType() {
        return this.result;
    }
    
    public String getMessage() {
        return this.message;
    }

    public boolean equals(Object obj)
    {
        if (obj == null || (obj.getClass() != this.getClass())) {
            return false;
        }
        OOPResultImpl other = (OOPResultImpl) obj;

        return (other.getResultType().equals(this.result) && other.getMessage().equals(this.message));
    }
}

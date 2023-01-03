package OOP.Solution;

import java.util.*;
import OOP.Provided.*;
import OOP.Provided.OOPResult.OOPTestResult;

public class OOPTestSummary {
    private HashMap<String, OOPResult> resultMap;

    public OOPTestSummary(Map<String,OOPResult> testMap)
    {
        resultMap = new HashMap<>(testMap);
    }

    public int getNumSuccesses()
    {
        return (int) this.resultMap.values().stream().filter(res -> res.getResultType() == OOPTestResult.SUCCESS)
                .count();
    }
    
    public int getNumFailures() {
        return (int) this.resultMap.values().stream().filter(res -> res.getResultType() == OOPTestResult.FAILURE)
                .count();
    }

    public int getNumExceptionMismatches() {
        return (int) this.resultMap.values().stream().filter(res -> res.getResultType() == OOPTestResult.EXPECTED_EXCEPTION_MISMATCH)
                .count();
    }

    public int getNumErrors() {
        return (int) this.resultMap.values().stream().filter(res -> res.getResultType() == OOPTestResult.ERROR)
                .count();
    } 
}

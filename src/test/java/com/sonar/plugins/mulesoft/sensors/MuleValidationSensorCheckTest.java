package com.sonar.plugins.mulesoft.sensors;

import com.sonar.plugins.mulesoft.check.MuleCheckList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MuleValidationSensorCheckTest {

    @Test
    void mule_check_list_has_three_checks_after_task7() {
        assertEquals(3, MuleCheckList.allChecks().size(),
            "MuleCheckList has 3 checks registered in Task 7");
    }
}

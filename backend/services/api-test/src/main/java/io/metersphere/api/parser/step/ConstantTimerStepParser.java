package io.metersphere.api.parser.step;

import io.metersphere.api.domain.ApiScenarioStep;
import io.metersphere.api.dto.request.controller.MsConstantTimerController;
import io.metersphere.api.dto.scenario.ApiScenarioStepCommonDTO;
import io.metersphere.plugin.api.spi.AbstractMsTestElement;

public class ConstantTimerStepParser extends StepParser {
    @Override
    public AbstractMsTestElement parseTestElement(ApiScenarioStepCommonDTO step, String resourceBlob, String stepDetail) {
        return parseConfig2TestElement(step, MsConstantTimerController.class);
    }

    @Override
    public Object parseDetail(ApiScenarioStep step) {
        return null;
    }
}

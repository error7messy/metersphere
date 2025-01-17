package io.metersphere.api.service.definition;

import io.metersphere.api.domain.ApiTestCase;
import io.metersphere.api.domain.ApiTestCaseExample;
import io.metersphere.api.dto.definition.ApiTestCaseAddRequest;
import io.metersphere.api.dto.definition.ApiTestCaseBatchRequest;
import io.metersphere.api.dto.definition.ApiTestCaseUpdateRequest;
import io.metersphere.api.mapper.ApiTestCaseMapper;
import io.metersphere.sdk.util.BeanUtils;
import io.metersphere.sdk.util.SubListUtils;
import io.metersphere.system.dto.sdk.ApiDefinitionCaseDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ApiTestCaseNoticeService {

    @Resource
    private ApiTestCaseService apiTestCaseService;
    @Resource
    private ApiTestCaseMapper apiTestCaseMapper;

    public ApiDefinitionCaseDTO getCaseDTO(ApiTestCaseAddRequest request) {
        ApiDefinitionCaseDTO caseDTO = new ApiDefinitionCaseDTO();
        BeanUtils.copyBean(caseDTO, request);
        caseDTO.setCaseName(request.getName());
        caseDTO.setCaseStatus(request.getStatus());
        caseDTO.setCaseCreateUser(null);
        caseDTO.setCaseUpdateUser(null);
        return caseDTO;
    }

    public ApiDefinitionCaseDTO getCaseDTO(ApiTestCaseUpdateRequest request) {
        ApiDefinitionCaseDTO caseDTO = new ApiDefinitionCaseDTO();
        BeanUtils.copyBean(caseDTO, request);
        caseDTO.setName(null);
        caseDTO.setStatus(null);
        caseDTO.setCaseCreateUser(null);
        caseDTO.setCaseUpdateUser(null);
        caseDTO.setCaseName(request.getName());
        caseDTO.setCaseStatus(request.getStatus());
        return caseDTO;
    }

    public ApiDefinitionCaseDTO getCaseDTO(String id) {
        ApiTestCase testCase = apiTestCaseMapper.selectByPrimaryKey(id);
        ApiDefinitionCaseDTO caseDTO = new ApiDefinitionCaseDTO();
        BeanUtils.copyBean(caseDTO, testCase);
        caseDTO.setName(null);
        caseDTO.setStatus(null);
        caseDTO.setCaseCreateTime(testCase.getCreateTime());
        caseDTO.setCaseUpdateTime(testCase.getUpdateTime());
        caseDTO.setCaseCreateUser(testCase.getCreateUser());
        caseDTO.setCaseUpdateUser(testCase.getUpdateUser());
        caseDTO.setCaseName(testCase.getName());
        caseDTO.setCaseStatus(testCase.getStatus());
        return caseDTO;
    }


    private List<ApiDefinitionCaseDTO> handleBatchNotice(List<String> ids) {
        List<ApiDefinitionCaseDTO> dtoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            SubListUtils.dealForSubList(ids, 500, subList -> {
                ApiTestCaseExample example = new ApiTestCaseExample();
                example.createCriteria().andIdIn(subList);
                List<ApiTestCase> caseList = apiTestCaseMapper.selectByExample(example);
                caseList.forEach(apiTestCase -> {
                    ApiDefinitionCaseDTO apiDefinitionCaseDTO = new ApiDefinitionCaseDTO();
                    apiDefinitionCaseDTO.setCaseName(apiTestCase.getName());
                    apiDefinitionCaseDTO.setProjectId(apiTestCase.getProjectId());
                    apiDefinitionCaseDTO.setCaseStatus(apiTestCase.getStatus());
                    apiDefinitionCaseDTO.setCreateUser(null);
                    dtoList.add(apiDefinitionCaseDTO);
                });
            });
        }
        return dtoList;
    }

    public List<ApiDefinitionCaseDTO> getBatchEditApiCaseDTO(ApiTestCaseBatchRequest request) {
        List<String> ids = apiTestCaseService.doSelectIds(request, false);
        return handleBatchNotice(ids);
    }
}

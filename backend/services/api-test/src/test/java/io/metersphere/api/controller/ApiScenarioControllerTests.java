package io.metersphere.api.controller;

import io.metersphere.api.constants.ApiDefinitionStatus;
import io.metersphere.api.constants.ApiScenarioStatus;
import io.metersphere.api.constants.ApiScenarioStepRefType;
import io.metersphere.api.constants.ApiScenarioStepType;
import io.metersphere.api.domain.*;
import io.metersphere.api.dto.definition.ApiDefinitionAddRequest;
import io.metersphere.api.dto.definition.ApiTestCaseAddRequest;
import io.metersphere.api.dto.request.assertion.MsAssertionConfig;
import io.metersphere.api.dto.request.assertion.MsScriptAssertion;
import io.metersphere.api.dto.request.http.MsHTTPElement;
import io.metersphere.api.dto.scenario.*;
import io.metersphere.api.mapper.*;
import io.metersphere.api.service.BaseResourcePoolTestService;
import io.metersphere.api.service.definition.ApiDefinitionService;
import io.metersphere.api.service.definition.ApiTestCaseService;
import io.metersphere.api.service.scenario.ApiScenarioService;
import io.metersphere.api.utils.ApiDataUtils;
import io.metersphere.project.dto.filemanagement.request.FileUploadRequest;
import io.metersphere.project.mapper.ExtBaseProjectVersionMapper;
import io.metersphere.project.service.FileMetadataService;
import io.metersphere.sdk.constants.ApplicationNumScope;
import io.metersphere.sdk.constants.DefaultRepositoryDir;
import io.metersphere.sdk.constants.PermissionConstants;
import io.metersphere.sdk.domain.Environment;
import io.metersphere.sdk.domain.EnvironmentExample;
import io.metersphere.sdk.domain.EnvironmentGroup;
import io.metersphere.sdk.file.FileCenter;
import io.metersphere.sdk.file.FileRequest;
import io.metersphere.sdk.mapper.EnvironmentGroupMapper;
import io.metersphere.sdk.mapper.EnvironmentMapper;
import io.metersphere.sdk.util.BeanUtils;
import io.metersphere.sdk.util.JSON;
import io.metersphere.system.base.BaseTest;
import io.metersphere.system.controller.handler.ResultHolder;
import io.metersphere.system.log.constants.OperationLogType;
import io.metersphere.system.uid.IDGenerator;
import io.metersphere.system.uid.NumGenerator;
import io.metersphere.system.utils.Pager;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.metersphere.api.controller.result.ApiResultCode.API_SCENARIO_EXIST;
import static io.metersphere.system.controller.handler.result.MsHttpResultCode.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiScenarioControllerTests extends BaseTest {
    private static final String BASE_PATH = "/api/scenario/";
    private static final String TRASH_PAGE = "trash/page";
    private static final String BATCH_EDIT = "batch/edit";
    private static final String FOLLOW = "follow/";
    protected static final String UPLOAD_TEMP_FILE = "upload/temp/file";
    protected static final String DELETE_TO_GC = "delete-to-gc/{0}";
    protected static final String STEP_GET = "step/get/{0}";
    protected static final String DEBUG = "debug";
    private static final String UPDATE_STATUS = "update-status";
    private static final String UPDATE_PRIORITY = "update-priority";

    private static final ResultMatcher ERROR_REQUEST_MATCHER = status().is5xxServerError();
    @Resource
    private ApiScenarioMapper apiScenarioMapper;
    @Resource
    private ApiScenarioModuleMapper apiScenarioModuleMapper;
    @Resource
    private ExtApiScenarioMapper extApiScenarioMapper;
    @Resource
    private EnvironmentMapper environmentMapper;
    @Resource
    private ApiScenarioFollowerMapper apiScenarioFollowerMapper;
    @Resource
    private EnvironmentGroupMapper environmentGroupMapper;
    @Resource
    private ApiScenarioStepMapper apiScenarioStepMapper;
    @Resource
    private ApiScenarioStepBlobMapper apiScenarioStepBlobMapper;
    @Resource
    private ApiScenarioBlobMapper apiScenarioBlobMapper;
    @Resource
    private ExtBaseProjectVersionMapper extBaseProjectVersionMapper;
    @Resource
    private ApiDefinitionService apiDefinitionService;
    @Resource
    private ApiTestCaseService apiTestCaseService;
    @Resource
    private BaseResourcePoolTestService baseResourcePoolTestService;
    @Resource
    private FileMetadataService fileMetadataService;
    @Resource
    private ApiScenarioCsvMapper apiScenarioCsvMapper;
    @Resource
    private ApiScenarioService apiScenarioService;
    private static String fileMetadataId;
    private static String localFileId;
    private static ApiScenario addApiScenario;
    private static List<ApiScenarioStepRequest> addApiScenarioSteps;
    private static ApiScenario anOtherAddApiScenario;
    private static List<ApiScenarioStepRequest> anOtherAddApiScenarioSteps;
    private static ApiDefinition apiDefinition;
    private static ApiTestCase apiTestCase;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    public void initApiScenario() {
        ApiScenarioModule apiScenarioModule = new ApiScenarioModule();
        apiScenarioModule.setId("scenario-moduleId");
        apiScenarioModule.setProjectId(DEFAULT_PROJECT_ID);
        apiScenarioModule.setName("场景模块");
        apiScenarioModule.setCreateTime(System.currentTimeMillis());
        apiScenarioModule.setUpdateTime(System.currentTimeMillis());
        apiScenarioModule.setCreateUser("admin");
        apiScenarioModule.setUpdateUser("admin");
        apiScenarioModule.setPos(0L);
        apiScenarioModuleMapper.insertSelective(apiScenarioModule);

        // 创建环境组
        EnvironmentGroup environmentGroup = new EnvironmentGroup();
        environmentGroup.setId("scenario-environment-group-id");
        environmentGroup.setProjectId(DEFAULT_PROJECT_ID);
        environmentGroup.setName("环境组");
        environmentGroup.setCreateTime(System.currentTimeMillis());
        environmentGroup.setUpdateTime(System.currentTimeMillis());
        environmentGroup.setCreateUser("admin");
        environmentGroup.setUpdateUser("admin");
        environmentGroup.setPos(0L);
        environmentGroupMapper.insertSelective(environmentGroup);

        EnvironmentExample environmentExample = new EnvironmentExample();
        environmentExample.createCriteria().andProjectIdEqualTo(DEFAULT_PROJECT_ID).andMockEqualTo(true);
        List<Environment> environments = environmentMapper.selectByExample(environmentExample);

        for (int i = 0; i < 10; i++) {
            ApiScenario apiScenario = new ApiScenario();
            apiScenario.setId("api-scenario-id" + i);
            apiScenario.setProjectId(DEFAULT_PROJECT_ID);
            apiScenario.setName(StringUtils.join("接口场景", apiScenario.getId()));
            apiScenario.setModuleId("scenario-moduleId");
            apiScenario.setStatus("未规划");
            apiScenario.setNum(NumGenerator.nextNum(DEFAULT_PROJECT_ID, ApplicationNumScope.API_SCENARIO));
            apiScenario.setPos(0L);
            apiScenario.setPriority("P0");
            apiScenario.setLatest(true);
            apiScenario.setVersionId("1.0");
            apiScenario.setRefId(apiScenario.getId());
            apiScenario.setCreateTime(System.currentTimeMillis());
            apiScenario.setUpdateTime(System.currentTimeMillis());
            apiScenario.setCreateUser("admin");
            apiScenario.setUpdateUser("admin");
            if (i % 2 == 0) {
                apiScenario.setTags(new ArrayList<>(List.of("tag1", "tag2")));
                apiScenario.setGrouped(true);
                apiScenario.setEnvironmentId("scenario-environment-group-id");
            } else {
                apiScenario.setGrouped(false);
                apiScenario.setEnvironmentId(environments.get(0).getId());
            }
            apiScenarioMapper.insertSelective(apiScenario);
        }
    }

    @Test
    @Order(0)
    public void uploadTempFileTest() throws Exception {
        // 准备数据，上传文件管理文件
        uploadFileMetadata();
        MockMultipartFile file = getMockMultipartFile();
        localFileId = doUploadTempFile(file);

    }

    public void initApiScenarioTrash() {
        ApiScenarioModule apiScenarioModule = new ApiScenarioModule();
        apiScenarioModule.setId("scenario-moduleId-trash");
        apiScenarioModule.setProjectId(DEFAULT_PROJECT_ID);
        apiScenarioModule.setName("场景模块-trash");
        apiScenarioModule.setCreateTime(System.currentTimeMillis());
        apiScenarioModule.setUpdateTime(System.currentTimeMillis());
        apiScenarioModule.setCreateUser("admin");
        apiScenarioModule.setUpdateUser("admin");
        apiScenarioModule.setPos(0L);
        apiScenarioModuleMapper.insertSelective(apiScenarioModule);
        for (int i = 0; i < 10; i++) {
            ApiScenario apiScenario = new ApiScenario();
            apiScenario.setId("trash-api-scenario-id" + i);
            apiScenario.setProjectId(DEFAULT_PROJECT_ID);
            apiScenario.setName(StringUtils.join("接口场景-回收站", apiScenario.getId()));
            apiScenario.setModuleId("scenario-moduleId-trash");
            apiScenario.setStatus("未规划");
            apiScenario.setNum(NumGenerator.nextNum(DEFAULT_PROJECT_ID, ApplicationNumScope.API_SCENARIO));
            apiScenario.setPos(0L);
            apiScenario.setLatest(true);
            apiScenario.setPriority("P0");
            apiScenario.setVersionId("1.0");
            apiScenario.setRefId(apiScenario.getId());
            apiScenario.setCreateTime(System.currentTimeMillis());
            apiScenario.setUpdateTime(System.currentTimeMillis());
            apiScenario.setCreateUser("admin");
            apiScenario.setUpdateUser("admin");
            apiScenarioMapper.insertSelective(apiScenario);
        }
    }

    @Test
    @Order(1)
    public void add() throws Exception {
        initTestData();

        // @@请求成功
        ApiScenarioAddRequest request = new ApiScenarioAddRequest();
        request.setProjectId(DEFAULT_PROJECT_ID);
        request.setDescription("desc");
        request.setName("test name");
        request.setModuleId("default");
        request.setGrouped(false);
        request.setEnvironmentId("environmentId");
        request.setTags(List.of("tag1", "tag2"));
        request.setPriority("P0");
        request.setStatus(ApiScenarioStatus.COMPLETED.name());
        List<ApiScenarioStepRequest> steps = getApiScenarioStepRequests();
        Map<String, Object> steptDetailMap = new HashMap<>();
        steptDetailMap.put(steps.get(1).getId(), getMsHttpElementParam());
        steptDetailMap.put(steps.get(0).getId(), getMsHttpElementParam());
        request.setSteps(steps);
        request.setStepDetails(steptDetailMap);
        request.setScenarioConfig(getScenarioConfig());

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        ApiScenario resultData = getResultData(mvcResult, ApiScenario.class);
        this.addApiScenario = apiScenarioMapper.selectByPrimaryKey(resultData.getId());
        assertUpdateApiScenario(request, request.getScenarioConfig(), addApiScenario.getId());
        assertUpdateSteps(steps, steptDetailMap);

        request.setName("anOther name");
        ApiScenarioStepRequest stepRequest = new ApiScenarioStepRequest();
        stepRequest.setId(IDGenerator.nextStr());
        stepRequest.setEnable(true);
        stepRequest.setName(addApiScenario.getName());
        stepRequest.setResourceId(addApiScenario.getId());
        stepRequest.setRefType(ApiScenarioStepRefType.REF.name());
        stepRequest.setStepType(ApiScenarioStepType.API_SCENARIO.name());

        ApiScenarioStepRequest stepRequest2 = new ApiScenarioStepRequest();
        stepRequest2.setId(IDGenerator.nextStr());
        stepRequest2.setName(addApiScenario.getName());
        stepRequest2.setResourceId(addApiScenario.getId());
        stepRequest2.setRefType(ApiScenarioStepRefType.PARTIAL_REF.name());
        stepRequest2.setChildren(List.of(stepRequest));
        stepRequest2.setStepType(ApiScenarioStepType.API_SCENARIO.name());
        steps = List.of(stepRequest, stepRequest2);
        request.setSteps(steps);
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        this.anOtherAddApiScenario = apiScenarioMapper.selectByPrimaryKey(getResultData(mvcResult, ApiScenario.class).getId());
        assertUpdateApiScenario(request, request.getScenarioConfig(), anOtherAddApiScenario.getId());
        assertUpdateSteps(steps, steptDetailMap);
        anOtherAddApiScenarioSteps = steps;

        // @@重名校验异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), API_SCENARIO_EXIST);
        // @@校验日志
        checkLog(this.addApiScenario.getId(), OperationLogType.ADD);
        // @@校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_ADD, DEFAULT_ADD, request);
    }

    private Object getMsHttpElementParam() {
        return JSON.parseObject(ApiDataUtils.toJSONString(MsHTTPElementTest.getMsHttpElement()));
    }

    /**
     * 校验更新数据
     *
     * @param request
     * @param scenarioConfig
     * @param id
     * @return
     * @throws Exception
     */
    private ApiScenario assertUpdateApiScenario(Object request, ScenarioConfig scenarioConfig, String id) {
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(id);
        ApiScenarioBlob apiScenarioBlob = apiScenarioBlobMapper.selectByPrimaryKey(id);
        ApiScenario copyApiScenario = BeanUtils.copyBean(new ApiScenario(), apiScenario);
        copyApiScenario = BeanUtils.copyBean(copyApiScenario, request);
        Assertions.assertEquals(apiScenario, copyApiScenario);
        if (scenarioConfig != null) {
            Assertions.assertEquals(scenarioConfig, JSON.parseObject(new String(apiScenarioBlob.getConfig()), ScenarioConfig.class));
        }
        return apiScenario;
    }

    private void assertUpdateSteps(List<ApiScenarioStepRequest> steps, Map<String, Object> steptDetailMap) {
        for (ApiScenarioStepRequest step : steps) {
            ApiScenarioStep apiScenarioStep = apiScenarioStepMapper.selectByPrimaryKey(step.getId());
            ApiScenarioStep copyApiScenarioStep = BeanUtils.copyBean(new ApiScenarioStep(), apiScenarioStep);
            copyApiScenarioStep = BeanUtils.copyBean(copyApiScenarioStep, step);
            Assertions.assertEquals(apiScenarioStep, copyApiScenarioStep);
            ApiScenarioStepBlob apiScenarioStepBlob = apiScenarioStepBlobMapper.selectByPrimaryKey(step.getId());
            if (apiScenarioStepBlob != null && steptDetailMap.get(step.getId()) != null) {
                Assertions.assertEquals(steptDetailMap.get(step.getId()), JSON.parseObject(new String(apiScenarioStepBlob.getContent())));
            }
        }
    }

    private ScenarioConfig getScenarioConfig() throws Exception {
        ScenarioConfig scenarioConfig = new ScenarioConfig();
        MsAssertionConfig msAssertionConfig = new MsAssertionConfig();
        MsScriptAssertion scriptAssertion = new MsScriptAssertion();
        scriptAssertion.setScript("{}");
        scriptAssertion.setName("script");
        msAssertionConfig.setAssertions(List.of(scriptAssertion));
        scenarioConfig.setAssertionConfig(msAssertionConfig);
        ScenarioOtherConfig scenarioOtherConfig = new ScenarioOtherConfig();
        scenarioOtherConfig.setStepWaitTime(1000);
        scenarioOtherConfig.setFailureStrategy(ScenarioOtherConfig.FailureStrategy.CONTINUE.name());
        scenarioOtherConfig.setEnableCookieShare(true);
        scenarioConfig.setOtherConfig(scenarioOtherConfig);
        ScenarioVariable scenarioVariable = new ScenarioVariable();
        scenarioVariable.setCsvVariables(getCsvVariables());
        scenarioConfig.setVariable(scenarioVariable);
        return scenarioConfig;
    }

    /**
     * 文件管理插入一条数据
     * 便于测试关联文件
     */
    private void uploadFileMetadata() throws Exception {
        FileUploadRequest fileUploadRequest = new FileUploadRequest();
        fileUploadRequest.setProjectId(DEFAULT_PROJECT_ID);
        //导入正常文件
        MockMultipartFile file = new MockMultipartFile("file", "file.csv", MediaType.APPLICATION_OCTET_STREAM_VALUE, "aa".getBytes());
        fileMetadataId = fileMetadataService.upload(fileUploadRequest, "admin", file);
    }

    public List<CsvVariable> getCsvVariables() throws Exception {
        List<CsvVariable> csvVariables = new ArrayList<>();
        CsvVariable csvVariable = new CsvVariable();
        csvVariable.setFileId(localFileId);
        csvVariable.setName("csv变量");
        csvVariable.setScope(CsvVariable.CsvVariableScope.SCENARIO.name());
        csvVariables.add(csvVariable);
        csvVariable = new CsvVariable();
        csvVariable.setFileId(fileMetadataId);
        csvVariable.setName("csv-关联的");
        csvVariable.setScope(CsvVariable.CsvVariableScope.SCENARIO.name());
        csvVariable.setAssociation(true);
        csvVariables.add(csvVariable);
        return csvVariables;
    }

    private List<ApiScenarioStepRequest> getApiScenarioStepRequests() {
        ApiScenarioStepRequest stepRequest = new ApiScenarioStepRequest();
        stepRequest.setId(IDGenerator.nextStr());
        stepRequest.setVersionId(extBaseProjectVersionMapper.getDefaultVersion(DEFAULT_PROJECT_ID));
        stepRequest.setConfig(new HashMap<>());
        stepRequest.setEnable(true);
        stepRequest.setName(apiTestCase.getName());
        stepRequest.setResourceId(apiTestCase.getId());
        stepRequest.setRefType(ApiScenarioStepRefType.REF.name());
        stepRequest.setStepType(ApiScenarioStepType.API_CASE.name());
        stepRequest.setProjectId(DEFAULT_PROJECT_ID);
        stepRequest.setConfig(new HashMap<>());
        stepRequest.setCsvFileIds(List.of(fileMetadataId));

        ApiScenarioStepRequest stepRequest2 = new ApiScenarioStepRequest();
        stepRequest2.setId(IDGenerator.nextStr());
        stepRequest2.setVersionId(extBaseProjectVersionMapper.getDefaultVersion(DEFAULT_PROJECT_ID));
        stepRequest2.setConfig(new HashMap<>());
        stepRequest2.setEnable(true);
        stepRequest2.setResourceId(apiTestCase.getId());
        stepRequest2.setName(apiTestCase.getName() + "2");
        stepRequest2.setStepType(ApiScenarioStepType.API_CASE.name());
        stepRequest2.setRefType(ApiScenarioStepRefType.COPY.name());
        stepRequest2.setProjectId(DEFAULT_PROJECT_ID);
        stepRequest2.setCsvFileIds(List.of(fileMetadataId));

        ApiScenarioStepRequest stepRequest3 = new ApiScenarioStepRequest();
        stepRequest3.setId(IDGenerator.nextStr());
        stepRequest3.setVersionId(extBaseProjectVersionMapper.getDefaultVersion(DEFAULT_PROJECT_ID));
        stepRequest3.setConfig(new HashMap<>());
        stepRequest3.setEnable(true);
        stepRequest3.setStepType(ApiScenarioStepType.API.name());
        stepRequest3.setResourceId(apiDefinition.getId());
        stepRequest3.setName(apiDefinition.getName() + "3");
        stepRequest3.setRefType(ApiScenarioStepRefType.REF.name());
        stepRequest3.setProjectId(DEFAULT_PROJECT_ID);

        return new ArrayList<>() {{
            add(stepRequest);
            add(stepRequest2);
            add(stepRequest3);
        }};
    }

    private void initTestData() {
        ApiDefinitionAddRequest apiDefinitionAddRequest = new ApiDefinitionAddRequest();
        apiDefinitionAddRequest.setName("test scenario");
        apiDefinitionAddRequest.setProtocol("HTTP");
        apiDefinitionAddRequest.setProjectId(DEFAULT_PROJECT_ID);
        apiDefinitionAddRequest.setMethod("POST");
        apiDefinitionAddRequest.setPath("/api/admin/posts");
        apiDefinitionAddRequest.setStatus(ApiDefinitionStatus.PREPARE.getValue());
        apiDefinitionAddRequest.setModuleId("default");
        apiDefinitionAddRequest.setVersionId(extBaseProjectVersionMapper.getDefaultVersion(DEFAULT_PROJECT_ID));
        apiDefinitionAddRequest.setDescription("描述内容");
        apiDefinitionAddRequest.setName("test scenario");
        MsHTTPElement msHttpElement = MsHTTPElementTest.getMsHttpElement();
        apiDefinitionAddRequest.setRequest(ApiDataUtils.toJSONString(msHttpElement));
        apiDefinitionAddRequest.setResponse("{}");
        apiDefinition = apiDefinitionService.create(apiDefinitionAddRequest, "admin");

        ApiTestCaseAddRequest apiTestCaseAddRequest = new ApiTestCaseAddRequest();
        apiTestCaseAddRequest.setApiDefinitionId(apiDefinition.getId());
        apiTestCaseAddRequest.setName("test");
        apiTestCaseAddRequest.setProjectId(DEFAULT_PROJECT_ID);
        apiTestCaseAddRequest.setPriority("P0");
        apiTestCaseAddRequest.setStatus("Underway");
        apiTestCaseAddRequest.setTags(new LinkedHashSet<>(List.of("tag1", "tag2")));
        apiTestCaseAddRequest.setRequest(ApiDataUtils.toJSONString(msHttpElement));
        apiTestCase = apiTestCaseService.addCase(apiTestCaseAddRequest, "admin");
    }


    @Test
    @Order(2)
    public void update() throws Exception {
        // @@请求成功
        ApiScenarioUpdateRequest request = new ApiScenarioUpdateRequest();
        request.setId(addApiScenario.getId());
        request.setDescription("desc update");
        request.setName("test name update");
        request.setModuleId("default");
        request.setGrouped(false);
        request.setEnvironmentId("environmentId update");
        request.setTags(List.of("tag1 update", "tag2 update"));
        request.setPriority("P0 update");
        request.setStatus(ApiScenarioStatus.DEPRECATED.name());
        List<ApiScenarioStepRequest> steps = getApiScenarioStepRequests();

        // 验证修改基础信息
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        assertUpdateApiScenario(request, request.getScenarioConfig(), addApiScenario.getId());

        // 验证清除步骤
        request.setSteps(List.of());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        assertUpdateApiScenario(request, request.getScenarioConfig(), addApiScenario.getId());
        assertUpdateSteps(List.of(), new HashMap<>());

        // 验证添加步骤
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        Map<String, Object> steptDetailMap = new HashMap<>();
        steptDetailMap.put(steps.get(0).getId(), getMsHttpElementParam());
        steptDetailMap.put(steps.get(1).getId(), getMsHttpElementParam());
        request.setSteps(steps);
        request.setStepDetails(steptDetailMap);
        request.setScenarioConfig(getScenarioConfig());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        assertUpdateSteps(steps, steptDetailMap);

        ApiScenarioCsvExample apiScenarioCsvExample = new ApiScenarioCsvExample();
        apiScenarioCsvExample.createCriteria().andScenarioIdEqualTo(addApiScenario.getId());
        List<ApiScenarioCsv> apiScenarioCsvs = apiScenarioCsvMapper.selectByExample(apiScenarioCsvExample);
        Map<String, ApiScenarioCsv> collect = apiScenarioCsvs.stream().collect(Collectors.toMap(ApiScenarioCsv::getFileId, t -> t));
        // 验证修改步骤
        steps.get(0).setName("test name update");
        CsvVariable csvVariable = request.getScenarioConfig().getVariable().getCsvVariables().get(0);
        request.getScenarioConfig().getVariable().getCsvVariables().get(0).setId(collect.get(csvVariable.getFileId()).getId());
        CsvVariable csvVariable1 = request.getScenarioConfig().getVariable().getCsvVariables().get(0);
        request.getScenarioConfig().getVariable().getCsvVariables().get(1).setId(collect.get(csvVariable1.getFileId()).getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        assertUpdateSteps(steps, steptDetailMap);
        addApiScenarioSteps = steps;

        // @@重名校验异常
        request.setName(anOtherAddApiScenario.getName());
        assertErrorCode(this.requestPost(DEFAULT_UPDATE, request), API_SCENARIO_EXIST);
        // @@校验日志
        checkLog(request.getId(), OperationLogType.UPDATE);
        // @@校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(5)
    public void uploadTempFile() throws Exception {
        // @@请求成功
        MockMultipartFile file = getMockMultipartFile();
        String fileId = doUploadTempFile(file);

        // 校验文件存在
        FileRequest fileRequest = new FileRequest();
        fileRequest.setFolder(DefaultRepositoryDir.getSystemTempDir() + "/" + fileId);
        fileRequest.setFileName(file.getOriginalFilename());
        Assertions.assertNotNull(FileCenter.getDefaultRepository().getFile(fileRequest));

        requestUploadPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_ADD, UPLOAD_TEMP_FILE, file);
        requestUploadPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, UPLOAD_TEMP_FILE, file);
    }

    @Test
    @Order(6)
    public void debug() throws Exception {
        mockPost("/api/debug", "");
        baseResourcePoolTestService.initProjectResourcePool();
        // @@请求成功
        ApiScenarioDebugRequest request = new ApiScenarioDebugRequest();
        request.setProjectId(DEFAULT_PROJECT_ID);
        request.setScenarioConfig(new ScenarioConfig());
        request.setEnvironmentId("environmentId");

        List<ApiScenarioStepRequest> steps = getApiScenarioStepRequests();
        ApiScenarioStepRequest stepRequest = new ApiScenarioStepRequest();
        stepRequest.setName("test step");
        stepRequest.setId(IDGenerator.nextStr());
        stepRequest.setRefType(ApiScenarioStepRefType.REF.name());
        stepRequest.setResourceId(addApiScenario.getId());
        stepRequest.setResourceNum(addApiScenario.getNum().toString());
        stepRequest.setName(addApiScenario.getName());
        stepRequest.setStepType(ApiScenarioStepType.API_SCENARIO.name());
        stepRequest.setChildren(getApiScenarioStepRequests());
        steps.add(stepRequest);
        ApiScenarioStepRequest copyScenarioStep = BeanUtils.copyBean(new ApiScenarioStepRequest(), stepRequest);
        copyScenarioStep.setRefType(ApiScenarioStepRefType.COPY.name());
        copyScenarioStep.setChildren(getApiScenarioStepRequests());
        steps.add(stepRequest);
        ApiScenarioStepRequest partialScenarioStep = BeanUtils.copyBean(new ApiScenarioStepRequest(), stepRequest);
        partialScenarioStep.setRefType(ApiScenarioStepRefType.PARTIAL_REF.name());
        partialScenarioStep.setChildren(getApiScenarioStepRequests());
        steps.add(partialScenarioStep);
        request.setId(addApiScenario.getId());
        request.setSteps(steps);
        request.setStepDetails(new HashMap<>());
        this.requestPostWithOk(DEBUG, request);

        // @@校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_EXECUTE, DEBUG, request);
    }

    @Test
    @Order(7)
    public void get() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addApiScenario.getId());
        ApiScenarioDetail apiScenarioDetail = getResultData(mvcResult, ApiScenarioDetail.class);
        // 验证数据
        asserGetApiScenarioSteps(this.addApiScenarioSteps, apiScenarioDetail.getSteps());

        mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, anOtherAddApiScenario.getId());
        apiScenarioDetail = getResultData(mvcResult, ApiScenarioDetail.class);
        // 验证数据
        Assertions.assertEquals(this.anOtherAddApiScenarioSteps.size(), apiScenarioDetail.getSteps().size());
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_READ, DEFAULT_GET, addApiScenario.getId());
    }

    @Test
    @Order(7)
    public void getStepDetail() throws Exception {
        ApiScenarioDetail apiScenarioDetail = apiScenarioService.get(addApiScenario.getId());
        List<ApiScenarioStepDTO> steps = apiScenarioDetail.getSteps();
        requestGetStepDetail(steps);

        apiScenarioDetail = apiScenarioService.get(anOtherAddApiScenario.getId());
        steps = apiScenarioDetail.getSteps();
        requestGetStepDetail(steps);

        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_READ, STEP_GET, addApiScenario.getId());
    }

    private void requestGetStepDetail(List<? extends ApiScenarioStepCommonDTO> steps) throws Exception {
        if (CollectionUtils.isEmpty(steps)) {
            return;
        }
        for (ApiScenarioStepCommonDTO step : steps) {
            this.requestGetWithOk(STEP_GET, step.getId());
            List<? extends ApiScenarioStepCommonDTO> children = step.getChildren();
            requestGetStepDetail(children);
        }
    }

    private void asserGetApiScenarioSteps(List<? extends ApiScenarioStepCommonDTO> addApiScenarioSteps, List<? extends ApiScenarioStepCommonDTO> steps) {
        if (addApiScenarioSteps == null || steps == null) {
            Assertions.assertEquals(addApiScenarioSteps, null);
            Assertions.assertEquals(steps, null);
            return;
        }
        Assertions.assertEquals(addApiScenarioSteps.size(), steps.size());
        for (int i = 0; i < addApiScenarioSteps.size(); i++) {
            ApiScenarioStepRequest stepRequest = (ApiScenarioStepRequest) addApiScenarioSteps.get(i);
            ApiScenarioStepDTO stepDTO = (ApiScenarioStepDTO) steps.get(i);
            Assertions.assertEquals(BeanUtils.copyBean(new ApiScenarioStepCommonDTO(), stepRequest), BeanUtils.copyBean(new ApiScenarioStepCommonDTO(), stepDTO));
            asserGetApiScenarioSteps(stepRequest.getChildren(), stepDTO.getChildren());
        }
    }

    private String doUploadTempFile(MockMultipartFile file) throws Exception {
        return JSON.parseObject(requestUploadFileWithOkAndReturn(UPLOAD_TEMP_FILE, file)
                        .getResponse()
                        .getContentAsString(), ResultHolder.class)
                .getData().toString();
    }

    private static MockMultipartFile getMockMultipartFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "file_upload.JPG",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "Hello, World!".getBytes()
        );
        return file;
    }

    @Test
    @Order(9)
    public void deleteToGc() throws Exception {
        // @@请求成功
        this.requestGetWithOk(DELETE_TO_GC, addApiScenario.getId());
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(addApiScenario.getId());
        Assertions.assertTrue(apiScenario.getDeleted());
        // @@校验日志
        checkLog(addApiScenario.getId(), OperationLogType.DELETE);
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_DELETE, DELETE_TO_GC, addApiScenario.getId());
    }

    @Test
    @Order(10)
    public void delete() throws Exception {
        // @@请求成功
        this.requestGetWithOk(DEFAULT_DELETE, addApiScenario.getId());
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(addApiScenario.getId());
        ApiScenarioBlob apiScenarioBlob = apiScenarioBlobMapper.selectByPrimaryKey(addApiScenario.getId());
        Assertions.assertNull(apiScenario);
        Assertions.assertNull(apiScenarioBlob);
        // @@校验日志
        checkLog(addApiScenario.getId(), OperationLogType.DELETE);
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_DELETE, DEFAULT_DELETE, addApiScenario.getId());
    }

    @Test
    @Order(11)
    public void page() throws Exception {
        // @@请求成功
        initApiScenario();
        ApiScenarioPageRequest pageRequest = new ApiScenarioPageRequest();
        pageRequest.setProjectId(DEFAULT_PROJECT_ID);
        pageRequest.setPageSize(10);
        pageRequest.setCurrent(1);
        MvcResult mvcResult = requestPostAndReturn(DEFAULT_PAGE, pageRequest);
        Pager<?> returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());
        //返回的数据量不超过规定要返回的数据量相同
        Assertions.assertTrue(((List<ApiScenarioDTO>) returnPager.getList()).size() <= pageRequest.getPageSize());

        //查询api-scenario-id1的数据
        pageRequest.setScenarioId("api-scenario-id1");
        mvcResult = requestPostAndReturn(DEFAULT_PAGE, pageRequest);
        returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());

        List<ApiScenarioDTO> scenarioDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ApiScenarioDTO.class);
        scenarioDTOS.forEach(caseDTO -> Assertions.assertEquals(caseDTO.getId(), "api-scenario-id1"));

        //查询模块为moduleId1的数据
        pageRequest.setScenarioId(null);
        pageRequest.setModuleIds(List.of("scenario-moduleId"));
        mvcResult = requestPostAndReturn(DEFAULT_PAGE, pageRequest);
        returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());
        scenarioDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ApiScenarioDTO.class);
        scenarioDTOS.forEach(scenario -> Assertions.assertEquals(scenario.getModuleId(), "scenario-moduleId"));

        pageRequest.setModuleIds(null);
        pageRequest.setSort(new HashMap<>() {{
            put("createTime", "asc");
        }});
        requestPostAndReturn(DEFAULT_PAGE, pageRequest);

        pageRequest = new ApiScenarioPageRequest();
        pageRequest.setProjectId(DEFAULT_PROJECT_ID);
        pageRequest.setPageSize(10);
        pageRequest.setCurrent(1);
        pageRequest.setModuleIds(List.of("scenario-moduleId"));
        pageRequest.setFilter(new HashMap<>() {{
            put("priority", List.of("P0"));
        }});
        mvcResult = requestPostAndReturn(DEFAULT_PAGE, pageRequest);
        returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());
        scenarioDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ApiScenarioDTO.class);
        scenarioDTOS.forEach(scenario -> Assertions.assertEquals(scenario.getPriority(), "P0"));

        //校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_READ, DEFAULT_PAGE, pageRequest);
    }

    @Test
    @Order(12)
    public void follow() throws Exception {
        // @@请求成功
        this.requestGetWithOk(FOLLOW + "api-scenario-id1");
        ApiScenarioFollowerExample example = new ApiScenarioFollowerExample();
        example.createCriteria().andApiScenarioIdEqualTo("api-scenario-id1").andUserIdEqualTo("admin");
        List<ApiScenarioFollower> followers = apiScenarioFollowerMapper.selectByExample(example);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(followers));
        // @@校验日志
        checkLog("api-scenario-id1", OperationLogType.UPDATE);
        assertErrorCode(this.requestGet(FOLLOW + "111"), NOT_FOUND);
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, FOLLOW + "api-scenario-id1");

        // @@请求成功
        this.requestGetWithOk(FOLLOW + "api-scenario-id1");
        example = new ApiScenarioFollowerExample();
        example.createCriteria().andApiScenarioIdEqualTo("api-scenario-id1").andUserIdEqualTo("admin");
        followers = apiScenarioFollowerMapper.selectByExample(example);
        Assertions.assertTrue(CollectionUtils.isEmpty(followers));
        // @@校验日志
        checkLog("api-scenario-id1", OperationLogType.UPDATE);
        assertErrorCode(this.requestGet(FOLLOW + "111"), NOT_FOUND);
    }

    @Test
    @Order(12)
    public void updateStatus() throws Exception {
        List<ApiScenario> apiScenarioList = apiScenarioMapper.selectByExample(new ApiScenarioExample());
        String scenarioId = apiScenarioList.getFirst().getId();
        // @@请求成功
        this.requestGetWithOk(UPDATE_STATUS + "/" + scenarioId + "/Underway");
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(scenarioId);
        Assertions.assertEquals(apiScenario.getStatus(), "Underway");
        // @@校验日志
        checkLog(scenarioId, OperationLogType.UPDATE);
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, UPDATE_STATUS + "/" + scenarioId + "/Underway");
    }

    @Test
    @Order(12)
    public void updatePriority() throws Exception {
        // @@请求成功
        this.requestGetWithOk(UPDATE_PRIORITY + "/" + "api-scenario-id1" + "/P1");
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey("api-scenario-id1");
        Assertions.assertEquals(apiScenario.getPriority(), "P1");
        // @@校验日志
        checkLog("api-scenario-id1", OperationLogType.UPDATE);
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, UPDATE_PRIORITY + "/" + "api-scenario-id1" + "/P1");
    }

    @Test
    @Order(13)
    public void batchEdit() throws Exception {
        // 追加标签
        ApiScenarioBatchEditRequest request = new ApiScenarioBatchEditRequest();
        request.setProjectId(DEFAULT_PROJECT_ID);
        request.setType("Tags");
        request.setAppendTag(true);
        request.setSelectAll(true);
        request.setTags(new LinkedHashSet<>(List.of("tag1", "tag3", "tag4")));
        requestPostAndReturn(BATCH_EDIT, request);
        ApiScenarioExample example = new ApiScenarioExample();
        List<String> ids = extApiScenarioMapper.getIds(request, false);
        example.createCriteria().andProjectIdEqualTo(DEFAULT_PROJECT_ID).andDeletedEqualTo(false).andIdIn(ids);
        apiScenarioMapper.selectByExample(example).forEach(apiTestCase -> {
            Assertions.assertTrue(apiTestCase.getTags().contains("tag1"));
            Assertions.assertTrue(apiTestCase.getTags().contains("tag3"));
            Assertions.assertTrue(apiTestCase.getTags().contains("tag4"));
        });
        //覆盖标签
        request.setTags(new LinkedHashSet<>(List.of("tag1")));
        request.setAppendTag(false);
        requestPostAndReturn(BATCH_EDIT, request);
        apiScenarioMapper.selectByExample(example).forEach(scenario -> {
            Assertions.assertEquals(scenario.getTags(), List.of("tag1"));
        });
        //标签为空  报错
        request.setTags(new LinkedHashSet<>());
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //ids为空的时候
        request.setTags(new LinkedHashSet<>(List.of("tag1")));
        request.setSelectAll(true);
        List<ApiScenario> apiScenarioList = apiScenarioMapper.selectByExample(example);
        //提取所有的id
        List<String> apiIdList = apiScenarioList.stream().map(ApiScenario::getId).toList();
        request.setSelectIds(apiIdList);
        request.setExcludeIds(apiIdList);
        requestPostAndReturn(BATCH_EDIT, request);

        //优先级
        request.setTags(new LinkedHashSet<>());
        request.setSelectAll(true);
        request.setType("Priority");
        request.setModuleIds(List.of("scenario-moduleId"));
        request.setPriority("P3");
        request.setExcludeIds(new ArrayList<>());
        requestPostAndReturn(BATCH_EDIT, request);
        //判断数据的优先级是不是P3
        example.clear();
        example.createCriteria().andProjectIdEqualTo(DEFAULT_PROJECT_ID).andDeletedEqualTo(false);
        List<ApiScenario> apiScenarios = apiScenarioMapper.selectByExample(example);

        apiScenarios.forEach(apiTestCase -> Assertions.assertEquals(apiTestCase.getPriority(), "P3"));
        //优先级数据为空
        request.setPriority(null);
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //ids为空的时候
        request.setPriority("P3");
        request.setSelectAll(false);
        request.setSelectIds(List.of("api-scenario-id1"));
        request.setExcludeIds(List.of("api-scenario-id1"));
        requestPostAndReturn(BATCH_EDIT, request);
        //状态
        request.setPriority(null);
        request.setType("Status");
        request.setStatus("Completed");
        request.setSelectAll(true);
        request.setExcludeIds(new ArrayList<>());
        requestPostAndReturn(BATCH_EDIT, request);
        //判断数据的状态是不是Completed
        apiScenarios = apiScenarioMapper.selectByExample(example);
        apiScenarios.forEach(apiTestCase -> Assertions.assertEquals(apiTestCase.getStatus(), "Completed"));
        //状态数据为空
        request.setStatus(null);
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //环境
        request.setStatus(null);
        request.setType("Environment");
        EnvironmentExample environmentExample = new EnvironmentExample();
        environmentExample.createCriteria().andProjectIdEqualTo(DEFAULT_PROJECT_ID).andMockEqualTo(true);
        List<Environment> environments = environmentMapper.selectByExample(environmentExample);
        request.setEnvId(environments.get(0).getId());
        requestPostAndReturn(BATCH_EDIT, request);
        //判断数据的环境是不是environments.get(0).getId()
        apiScenarios = apiScenarioMapper.selectByExample(example);
        apiScenarios.forEach(apiTestCase -> Assertions.assertEquals(apiTestCase.getEnvironmentId(), environments.get(0).getId()));

        //环境数据为空
        request.setEnvId(null);
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //环境不存在
        request.setEnvId("111");
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //环境组
        request.setGrouped(true);
        request.setGroupId("scenario-environment-group-id");
        requestPostAndReturn(BATCH_EDIT, request);
        apiScenarios = apiScenarioMapper.selectByExample(example);
        apiScenarios.forEach(apiTestCase -> {
            Assertions.assertEquals(apiTestCase.getGrouped(), true);
            Assertions.assertEquals(apiTestCase.getEnvironmentId(), "scenario-environment-group-id");
        });


        //环境组数据为空
        request.setGroupId(null);
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //环境组不存在
        request.setGroupId("111");
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);
        //类型错误
        request.setType("111");
        this.requestPost(BATCH_EDIT, request, ERROR_REQUEST_MATCHER);

        //校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_UPDATE, BATCH_EDIT, request);

    }

    @Test
    @Order(15)
    public void trashPage() throws Exception {
        // @@请求成功
        initApiScenarioTrash();
        ApiScenarioPageRequest pageRequest = new ApiScenarioPageRequest();
        pageRequest.setProjectId(DEFAULT_PROJECT_ID);
        pageRequest.setPageSize(10);
        pageRequest.setCurrent(1);
        MvcResult mvcResult = requestPostAndReturn(TRASH_PAGE, pageRequest);
        Pager<?> returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());
        //返回的数据量不超过规定要返回的数据量相同
        Assertions.assertTrue(((List<ApiScenarioDTO>) returnPager.getList()).size() <= pageRequest.getPageSize());

        //查询api-scenario-id1的数据
        pageRequest.setScenarioId("trash-api-scenario-id1");
        mvcResult = requestPostAndReturn(TRASH_PAGE, pageRequest);
        returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());

        List<ApiScenarioDTO> scenarioDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ApiScenarioDTO.class);
        scenarioDTOS.forEach(caseDTO -> Assertions.assertEquals(caseDTO.getId(), "trash-api-scenario-id1"));

        //查询模块为moduleId1的数据
        pageRequest.setScenarioId(null);
        pageRequest.setModuleIds(List.of("scenario-moduleId-trash"));
        mvcResult = requestPostAndReturn(TRASH_PAGE, pageRequest);
        returnPager = getResultData(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), pageRequest.getCurrent());
        scenarioDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ApiScenarioDTO.class);
        scenarioDTOS.forEach(scenario -> Assertions.assertEquals(scenario.getModuleId(), "scenario-moduleId-trash"));

        pageRequest.setModuleIds(null);
        pageRequest.setSort(new HashMap<>() {{
            put("createTime", "asc");
        }});
        requestPostAndReturn(TRASH_PAGE, pageRequest);
        //校验权限
        requestPostPermissionTest(PermissionConstants.PROJECT_API_SCENARIO_READ, TRASH_PAGE, pageRequest);
    }
}
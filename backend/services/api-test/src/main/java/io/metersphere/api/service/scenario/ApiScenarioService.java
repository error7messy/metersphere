package io.metersphere.api.service.scenario;

import io.metersphere.api.constants.ApiResourceType;
import io.metersphere.api.constants.ApiScenarioStepRefType;
import io.metersphere.api.constants.ApiScenarioStepType;
import io.metersphere.api.domain.*;
import io.metersphere.api.dto.debug.ApiFileResourceUpdateRequest;
import io.metersphere.api.dto.debug.ApiResourceRunRequest;
import io.metersphere.api.dto.request.MsScenario;
import io.metersphere.api.dto.scenario.*;
import io.metersphere.api.mapper.*;
import io.metersphere.api.parser.step.StepParser;
import io.metersphere.api.parser.step.StepParserFactory;
import io.metersphere.api.service.ApiExecuteService;
import io.metersphere.api.service.ApiFileResourceService;
import io.metersphere.api.service.definition.ApiDefinitionService;
import io.metersphere.api.service.definition.ApiTestCaseService;
import io.metersphere.plugin.api.spi.AbstractMsTestElement;
import io.metersphere.project.mapper.ExtBaseProjectVersionMapper;
import io.metersphere.project.service.ProjectService;
import io.metersphere.sdk.constants.ApiExecuteRunMode;
import io.metersphere.sdk.constants.ApplicationNumScope;
import io.metersphere.sdk.constants.DefaultRepositoryDir;
import io.metersphere.sdk.domain.Environment;
import io.metersphere.sdk.domain.EnvironmentExample;
import io.metersphere.sdk.domain.EnvironmentGroup;
import io.metersphere.sdk.domain.EnvironmentGroupExample;
import io.metersphere.sdk.exception.MSException;
import io.metersphere.sdk.mapper.EnvironmentGroupMapper;
import io.metersphere.sdk.mapper.EnvironmentMapper;
import io.metersphere.sdk.util.*;
import io.metersphere.system.log.constants.OperationLogModule;
import io.metersphere.system.service.UserLoginService;
import io.metersphere.system.uid.IDGenerator;
import io.metersphere.system.uid.NumGenerator;
import io.metersphere.system.utils.ServiceUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.metersphere.api.controller.result.ApiResultCode.API_SCENARIO_EXIST;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApiScenarioService {
    @Resource
    private ApiScenarioMapper apiScenarioMapper;

    @Resource
    private ExtApiScenarioMapper extApiScenarioMapper;

    @Resource
    private UserLoginService userLoginService;
    @Resource
    private ApiScenarioModuleMapper apiScenarioModuleMapper;
    @Resource
    private EnvironmentMapper environmentMapper;
    @Resource
    private EnvironmentGroupMapper environmentGroupMapper;
    @Resource
    private ApiScenarioLogService apiScenarioLogService;
    @Resource
    private ApiScenarioFollowerMapper apiScenarioFollowerMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ProjectService projectService;
    @Resource
    private ExtBaseProjectVersionMapper extBaseProjectVersionMapper;
    @Resource
    private ApiFileResourceService apiFileResourceService;
    @Resource
    private ApiScenarioStepMapper apiScenarioStepMapper;
    @Resource
    private ExtApiScenarioStepMapper extApiScenarioStepMapper;
    @Resource
    private ExtApiScenarioStepBlobMapper extApiScenarioStepBlobMapper;
    @Resource
    private ApiScenarioStepBlobMapper apiScenarioStepBlobMapper;
    @Resource
    private ApiScenarioBlobMapper apiScenarioBlobMapper;
    @Resource
    private ApiExecuteService apiExecuteService;
    @Resource
    private ApiDefinitionService apiDefinitionService;
    @Resource
    private ApiTestCaseService apiTestCaseService;
    public static final String PRIORITY = "Priority";
    public static final String STATUS = "Status";
    public static final String TAGS = "Tags";
    public static final String ENVIRONMENT = "Environment";


    public List<ApiScenarioDTO> getScenarioPage(ApiScenarioPageRequest request) {
        //CustomFieldUtils.setBaseQueryRequestCustomMultipleFields(request, userId);
        //TODO  场景的自定义字段 等设计 不一定会有
        List<ApiScenarioDTO> list = extApiScenarioMapper.list(request);
        if (!CollectionUtils.isEmpty(list)) {
            processApiScenario(list);
        }
        return list;
    }

    private void processApiScenario(List<ApiScenarioDTO> scenarioLists) {
        Set<String> userIds = extractUserIds(scenarioLists);
        Map<String, String> userMap = userLoginService.getUserNameMap(new ArrayList<>(userIds));
        List<String> envIds = scenarioLists.stream().map(ApiScenarioDTO::getEnvironmentId).toList();
        EnvironmentExample environmentExample = new EnvironmentExample();
        environmentExample.createCriteria().andIdIn(envIds);
        List<Environment> environments = environmentMapper.selectByExample(environmentExample);
        Map<String, String> envMap = environments.stream().collect(Collectors.toMap(Environment::getId, Environment::getName));
        EnvironmentGroupExample groupExample = new EnvironmentGroupExample();
        groupExample.createCriteria().andIdIn(envIds);
        List<EnvironmentGroup> environmentGroups = environmentGroupMapper.selectByExample(groupExample);
        Map<String, String> groupMap = environmentGroups.stream().collect(Collectors.toMap(EnvironmentGroup::getId, EnvironmentGroup::getName));
        //取模块id为新的set
        List<String> moduleIds = scenarioLists.stream().map(ApiScenarioDTO::getModuleId).distinct().toList();
        ApiScenarioModuleExample moduleExample = new ApiScenarioModuleExample();
        moduleExample.createCriteria().andIdIn(moduleIds);
        List<ApiScenarioModule> modules = apiScenarioModuleMapper.selectByExample(moduleExample);
        //生成map key为id value为name
        Map<String, String> moduleMap = modules.stream().collect(Collectors.toMap(ApiScenarioModule::getId, ApiScenarioModule::getName));
        scenarioLists.forEach(item -> {
            item.setCreateUserName(userMap.get(item.getCreateUser()));
            item.setDeleteUserName(userMap.get(item.getDeleteUser()));
            item.setUpdateUserName(userMap.get(item.getUpdateUser()));
            item.setModulePath(StringUtils.isNotBlank(moduleMap.get(item.getModuleId())) ? moduleMap.get(item.getModuleId()) : Translator.get("api_unplanned_scenario"));
            if (!item.getGrouped() && envMap.containsKey(item.getEnvironmentId())) {
                item.setEnvironmentName(envMap.get(item.getEnvironmentId()));
            } else if (item.getGrouped() && groupMap.containsKey(item.getId())) {
                item.setEnvironmentName(groupMap.get(item.getEnvironmentId()));
            }
        });
    }

    private Set<String> extractUserIds(List<ApiScenarioDTO> list) {
        return list.stream()
                .flatMap(apiScenario -> Stream.of(apiScenario.getUpdateUser(), apiScenario.getDeleteUser(), apiScenario.getCreateUser()))
                .collect(Collectors.toSet());
    }

    public void batchEdit(ApiScenarioBatchEditRequest request, String userId) {
        List<String> ids = doSelectIds(request, false);
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        SubListUtils.dealForSubList(ids, 500, subList -> batchEditByType(request, subList, userId, request.getProjectId()));
    }

    private void batchEditByType(ApiScenarioBatchEditRequest request, List<String> ids, String userId, String projectId) {
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andIdIn(ids);
        ApiScenario updateScenario = new ApiScenario();
        updateScenario.setUpdateUser(userId);
        updateScenario.setUpdateTime(System.currentTimeMillis());
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ApiScenarioMapper mapper = sqlSession.getMapper(ApiScenarioMapper.class);

        switch (request.getType()) {
            case PRIORITY -> batchUpdatePriority(example, updateScenario, request.getPriority());
            case STATUS -> batchUpdateStatus(example, updateScenario, request.getStatus());
            case TAGS -> batchUpdateTags(example, updateScenario, request, ids, sqlSession, mapper);
            case ENVIRONMENT -> batchUpdateEnvironment(example, updateScenario, request);
            default -> throw new MSException(Translator.get("batch_edit_type_error"));
        }
        List<ApiScenario> scenarioInfoByIds = extApiScenarioMapper.getInfoByIds(ids, false);
        apiScenarioLogService.batchEditLog(scenarioInfoByIds, userId, projectId);
    }

    private void batchUpdateEnvironment(ApiScenarioExample example, ApiScenario updateScenario,
                                        ApiScenarioBatchEditRequest request) {
        if (BooleanUtils.isFalse(request.isGrouped())) {
            if (StringUtils.isBlank(request.getEnvId())) {
                throw new MSException(Translator.get("environment_id_is_null"));
            }
            Environment environment = environmentMapper.selectByPrimaryKey(request.getEnvId());
            if (environment == null) {
                throw new MSException(Translator.get("environment_is_not_exist"));
            }
            updateScenario.setGrouped(false);
            updateScenario.setEnvironmentId(request.getEnvId());
        } else {
            if (StringUtils.isBlank(request.getGroupId())) {
                throw new MSException(Translator.get("environment_group_id_is_null"));
            }
            EnvironmentGroup environmentGroup = environmentGroupMapper.selectByPrimaryKey(request.getGroupId());
            if (environmentGroup == null) {
                throw new MSException(Translator.get("environment_group_is_not_exist"));
            }
            updateScenario.setGrouped(true);
            updateScenario.setEnvironmentId(request.getGroupId());
        }
        apiScenarioMapper.updateByExampleSelective(updateScenario, example);

    }

    private void batchUpdateTags(ApiScenarioExample example, ApiScenario updateScenario,
                                 ApiScenarioBatchEditRequest request, List<String> ids,
                                 SqlSession sqlSession, ApiScenarioMapper mapper) {
        if (CollectionUtils.isEmpty(request.getTags())) {
            throw new MSException(Translator.get("tags_is_null"));
        }
        if (request.isAppendTag()) {
            Map<String, ApiScenario> scenarioMap = extApiScenarioMapper.getTagsByIds(ids, false)
                    .stream()
                    .collect(Collectors.toMap(ApiScenario::getId, Function.identity()));
            if (MapUtils.isNotEmpty(scenarioMap)) {
                scenarioMap.forEach((k, v) -> {
                    if (CollectionUtils.isNotEmpty(v.getTags())) {
                        List<String> orgTags = v.getTags();
                        orgTags.addAll(request.getTags());
                        v.setTags(orgTags.stream().distinct().toList());
                    } else {
                        v.setTags(request.getTags());
                    }
                    v.setUpdateTime(updateScenario.getUpdateTime());
                    v.setUpdateUser(updateScenario.getUpdateUser());
                    mapper.updateByPrimaryKeySelective(v);
                });
                sqlSession.flushStatements();
                if (sqlSessionFactory != null) {
                    SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
                }
            }
        } else {
            updateScenario.setTags(request.getTags());
            apiScenarioMapper.updateByExampleSelective(updateScenario, example);
        }
    }

    private void batchUpdateStatus(ApiScenarioExample example, ApiScenario updateScenario, String status) {
        if (StringUtils.isBlank(status)) {
            throw new MSException(Translator.get("status_is_null"));
        }
        updateScenario.setStatus(status);
        apiScenarioMapper.updateByExampleSelective(updateScenario, example);
    }

    private void batchUpdatePriority(ApiScenarioExample example, ApiScenario updateScenario, String priority) {
        if (StringUtils.isBlank(priority)) {
            throw new MSException(Translator.get("priority_is_null"));
        }
        updateScenario.setPriority(priority);
        apiScenarioMapper.updateByExampleSelective(updateScenario, example);
    }

    public List<String> doSelectIds(ApiScenarioBatchEditRequest request, boolean deleted) {
        if (request.isSelectAll()) {
            List<String> ids = extApiScenarioMapper.getIds(request, deleted);
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                ids.removeAll(request.getExcludeIds());
            }
            return ids;
        } else {
            return request.getSelectIds();
        }
    }

    public void follow(String id, String userId) {
        checkResourceExist(id);
        ApiScenarioFollowerExample example = new ApiScenarioFollowerExample();
        example.createCriteria().andApiScenarioIdEqualTo(id).andUserIdEqualTo(userId);
        if (apiScenarioFollowerMapper.countByExample(example) > 0) {
            apiScenarioFollowerMapper.deleteByPrimaryKey(id, userId);
            apiScenarioLogService.unfollowLog(id, userId);
        } else {
            ApiScenarioFollower apiScenarioFollower = new ApiScenarioFollower();
            apiScenarioFollower.setApiScenarioId(id);
            apiScenarioFollower.setUserId(userId);
            apiScenarioFollowerMapper.insertSelective(apiScenarioFollower);
            apiScenarioLogService.followLog(id, userId);
        }
    }

    public ApiScenario add(ApiScenarioAddRequest request, String creator) {
        checkAddExist(request);
        ApiScenario scenario = getAddApiScenario(request, creator);
        apiScenarioMapper.insert(scenario);

        // 更新场景配置
        ApiScenarioBlob apiScenarioBlob = new ApiScenarioBlob();
        apiScenarioBlob.setId(scenario.getId());
        apiScenarioBlob.setConfig(JSON.toJSONString(request.getScenarioConfig()).getBytes());
        apiScenarioBlobMapper.insert(apiScenarioBlob);

        // 插入步骤
        if (CollectionUtils.isNotEmpty(request.getSteps())) {
            // 获取待添加的步骤
            List<ApiScenarioStep> steps = getApiScenarioSteps(null, request.getSteps());
            steps.forEach(step -> step.setScenarioId(scenario.getId()));
            // 获取待添加的步骤详情
            List<ApiScenarioStepBlob> apiScenarioStepsDetails = getPartialRefStepDetails(request.getSteps());
            apiScenarioStepsDetails.addAll(getUpdateStepDetails(steps, request.getStepDetails()));
            apiScenarioStepsDetails.forEach(step -> step.setScenarioId(scenario.getId()));

            if (CollectionUtils.isNotEmpty(steps)) {
                apiScenarioStepMapper.batchInsert(steps);
            }
            if (CollectionUtils.isNotEmpty(apiScenarioStepsDetails)) {
                apiScenarioStepBlobMapper.batchInsert(apiScenarioStepsDetails);
            }
        }

        // 处理文件
        ApiFileResourceUpdateRequest resourceUpdateRequest = getApiFileResourceUpdateRequest(scenario.getId(), scenario.getProjectId(), creator);
        resourceUpdateRequest.setUploadFileIds(request.getUploadFileIds());
        resourceUpdateRequest.setLinkFileIds(request.getLinkFileIds());
        apiFileResourceService.addFileResource(resourceUpdateRequest);
        return scenario;
    }

    private ApiScenario getAddApiScenario(ApiScenarioAddRequest request, String creator) {
        ApiScenario scenario = new ApiScenario();
        BeanUtils.copyBean(scenario, request);
        scenario.setId(IDGenerator.nextStr());
        scenario.setNum(getNextNum(request.getProjectId()));
        scenario.setPos(getNextOrder(request.getProjectId()));
        scenario.setLatest(true);
        scenario.setCreateUser(creator);
        scenario.setUpdateUser(creator);
        scenario.setCreateTime(System.currentTimeMillis());
        scenario.setUpdateTime(System.currentTimeMillis());
        scenario.setVersionId(extBaseProjectVersionMapper.getDefaultVersion(request.getProjectId()));
        scenario.setRefId(scenario.getId());
        scenario.setLastReportStatus(StringUtils.EMPTY);
        scenario.setDeleted(false);
        scenario.setRequestPassRate("0");
        scenario.setStepTotal(CollectionUtils.isEmpty(request.getSteps()) ? 0 : request.getSteps().size());
        return scenario;
    }

    public ApiScenario update(ApiScenarioUpdateRequest request, String updater) {
        checkResourceExist(request.getId());
        checkUpdateExist(request);
        // 更新基础信息
        ApiScenario scenario = BeanUtils.copyBean(new ApiScenario(), request);
        scenario.setUpdateUser(updater);
        scenario.setUpdateTime(System.currentTimeMillis());
        apiScenarioMapper.updateByPrimaryKeySelective(scenario);

        if (request.getScenarioConfig() != null) {
            // 更新场景配置
            ApiScenarioBlob apiScenarioBlob = new ApiScenarioBlob();
            apiScenarioBlob.setId(scenario.getId());
            apiScenarioBlob.setConfig(JSON.toJSONString(request.getScenarioConfig()).getBytes());
            apiScenarioBlobMapper.updateByPrimaryKeyWithBLOBs(apiScenarioBlob);
        }

        // 更新场景步骤
        updateApiScenarioStep(request, scenario);

        ApiScenario originScenario = apiScenarioMapper.selectByPrimaryKey(request.getId());
        // 处理文件
        ApiFileResourceUpdateRequest resourceUpdateRequest = getApiFileResourceUpdateRequest(scenario.getId(), originScenario.getProjectId(), updater);
        resourceUpdateRequest.setUploadFileIds(request.getUploadFileIds());
        resourceUpdateRequest.setLinkFileIds(request.getLinkFileIds());
        resourceUpdateRequest.setUnLinkRefIds(request.getUnLinkRefIds());
        resourceUpdateRequest.setDeleteFileIds(request.getDeleteFileIds());
        apiFileResourceService.updateFileResource(resourceUpdateRequest);

        return scenario;
    }

    /**
     * 更新场景步骤
     *
     * @param request
     * @param scenario
     */
    private void updateApiScenarioStep(ApiScenarioUpdateRequest request, ApiScenario scenario) {
        // steps 不为 null 则修改
        if (request.getSteps() != null) {
            if (CollectionUtils.isEmpty(request.getSteps())) {
                // 如果是空数组，则删除所有步骤
                deleteStepByScenarioId(scenario.getId());
                deleteStepDetailByScenarioId(scenario.getId());
                return;
            }

            // 获取待更新的步骤
            List<ApiScenarioStep> apiScenarioSteps = getApiScenarioSteps(null, request.getSteps());
            apiScenarioSteps.forEach(step -> step.setScenarioId(scenario.getId()));

            // 获取待更新的步骤详情
            List<ApiScenarioStepBlob> apiScenarioStepsDetails = getPartialRefStepDetails(request.getSteps());
            apiScenarioStepsDetails.addAll(getUpdateStepDetails(apiScenarioSteps, request.getStepDetails()));
            apiScenarioStepsDetails.forEach(step -> step.setScenarioId(scenario.getId()));

            List<String> stepIds = apiScenarioSteps.stream().map(ApiScenarioStep::getId).collect(Collectors.toList());
            List<String> originStepIds = extApiScenarioStepMapper.getStepIdsByScenarioId(scenario.getId());
            List<String> deleteStepIds = ListUtils.subtract(originStepIds, stepIds);

            // 步骤表-全部先删除再插入
            deleteStepByScenarioId(scenario.getId());
            apiScenarioStepMapper.batchInsert(apiScenarioSteps);

            // 详情表-删除已经删除的步骤详情
            SubListUtils.dealForSubList(deleteStepIds, 100, subIds -> {
                ApiScenarioStepBlobExample stepBlobExample = new ApiScenarioStepBlobExample();
                stepBlobExample.createCriteria().andIdIn(subIds);
                apiScenarioStepBlobMapper.deleteByExample(stepBlobExample);
            });

            // 查询原有的步骤详情
            Set<String> originStepDetailIds = extApiScenarioStepBlobMapper.getStepIdsByScenarioId(scenario.getId())
                    .stream().collect(Collectors.toSet());

            // 添加新增的步骤详情
            List<ApiScenarioStepBlob> addApiScenarioStepsDetails = apiScenarioStepsDetails.stream()
                    .filter(step -> !originStepDetailIds.contains(step.getId()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(addApiScenarioStepsDetails)) {
                apiScenarioStepBlobMapper.batchInsert(addApiScenarioStepsDetails);
            }
            // 更新原有的步骤详情
            apiScenarioStepsDetails.stream()
                    .filter(step -> originStepDetailIds.contains(step.getId()))
                    .forEach(apiScenarioStepBlobMapper::updateByPrimaryKeySelective);
        } else if (MapUtils.isNotEmpty(request.getStepDetails())) {
            // steps 为 null，stepDetails 不为 null，则只更新详情
            // 查询原有的步骤详情
            Set<String> originStepDetailIds = extApiScenarioStepBlobMapper.getStepIdsByScenarioId(scenario.getId())
                    .stream().collect(Collectors.toSet());
            // 更新原有的步骤详情
            request.getStepDetails().forEach((stepId, stepDetail) -> {
                if (originStepDetailIds.contains(stepId)) {
                    ApiScenarioStepBlob apiScenarioStepBlob = new ApiScenarioStepBlob();
                    apiScenarioStepBlob.setId(stepId);
                    apiScenarioStepBlob.setContent(JSON.toJSONString(stepDetail).getBytes());
                    apiScenarioStepBlobMapper.updateByPrimaryKeySelective(apiScenarioStepBlob);
                }
            });
        }
    }

    private void deleteStepDetailByScenarioId(String scenarioId) {
        ApiScenarioStepBlobExample blobExample = new ApiScenarioStepBlobExample();
        blobExample.createCriteria().andScenarioIdEqualTo(scenarioId);
        apiScenarioStepBlobMapper.deleteByExample(blobExample);
    }

    private void deleteStepByScenarioId(String scenarioId) {
        ApiScenarioStepExample example = new ApiScenarioStepExample();
        example.createCriteria().andScenarioIdEqualTo(scenarioId);
        apiScenarioStepMapper.deleteByExample(example);
    }

    /**
     * 获取待更新的 ApiScenarioStepBlob 列表
     *
     * @param apiScenarioSteps
     * @param stepDetails
     * @return
     */
    private List<ApiScenarioStepBlob> getUpdateStepDetails(List<ApiScenarioStep> apiScenarioSteps, Map<String, Object> stepDetails) {
        if (MapUtils.isEmpty(stepDetails)) {
            return Collections.emptyList();
        }

        Map<String, ApiScenarioStep> scenarioStepMap = apiScenarioSteps.stream()
                .collect(Collectors.toMap(ApiScenarioStep::getId, Function.identity()));

        List<ApiScenarioStepBlob> apiScenarioStepsDetails = new ArrayList<>();
        stepDetails.forEach((stepId, stepDetail) -> {
            ApiScenarioStep step = scenarioStepMap.get(stepId);
            if (step == null) {
                return;
            }
            if (!isRef(step.getRefType()) || StringUtils.equals(step.getRefType(), ApiScenarioStepType.API.name())) {
                // 非引用的步骤，如果有编辑内容，保存到blob表
                // 如果引用的是接口定义，也保存详情，因为应用接口定义允许修改参数值
                ApiScenarioStepBlob apiScenarioStepBlob = new ApiScenarioStepBlob();
                apiScenarioStepBlob.setId(stepId);
                apiScenarioStepBlob.setContent(JSON.toJSONString(stepDetail).getBytes());
                apiScenarioStepsDetails.add(apiScenarioStepBlob);
            }
        });
        return apiScenarioStepsDetails;
    }

    private boolean isRef(String refType) {
        return StringUtils.equalsAny(refType, ApiScenarioStepRefType.REF.name(), ApiScenarioStepRefType.PARTIAL_REF.name());
    }

    /**
     * 解析步骤树结构
     * 获取待更新的 ApiScenarioStep 列表
     *
     * @param parent
     * @param steps
     * @return
     */
    private List<ApiScenarioStep> getApiScenarioSteps(ApiScenarioStepRequest parent,
                                                            List<ApiScenarioStepRequest> steps) {

        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptyList();
        }
        List<ApiScenarioStep> apiScenarioSteps = new ArrayList<>();
        long sort = 1;
        for (ApiScenarioStepRequest step : steps) {
            ApiScenarioStep apiScenarioStep = new ApiScenarioStep();
            BeanUtils.copyBean(apiScenarioStep, step);
            apiScenarioStep.setSort(sort++);
            if (parent != null) {
                apiScenarioStep.setParentId(parent.getId());
            }
            if (step.getConfig() != null) {
                apiScenarioStep.setConfig(JSON.toJSONString(step.getConfig()));
            }
            apiScenarioSteps.add(apiScenarioStep);

            if (StringUtils.equalsAny(step.getRefType(), ApiScenarioStepRefType.REF.name(), ApiScenarioStepRefType.PARTIAL_REF.name())) {
                // 引用的步骤不解析子步骤
                continue;
            }
            // 解析子步骤
            apiScenarioSteps.addAll(getApiScenarioSteps(step, step.getChildren()));
        }
        return apiScenarioSteps;
    }

    /**
     * 解析步骤树结构
     * 获取待更新的 ApiScenarioStep 列表
     *
     * @param steps
     * @return
     */
    private List<ApiScenarioStepBlob> getPartialRefStepDetails(List<ApiScenarioStepRequest> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptyList();
        }
        List<ApiScenarioStepBlob> apiScenarioStepsDetails = new ArrayList<>();

        for (ApiScenarioStepRequest step : steps) {
            if (StringUtils.equals(step.getRefType(), ApiScenarioStepRefType.REF.name())) {
                // 引用的步骤不解析子步骤
                continue;
            }
            if (StringUtils.equals(step.getRefType(), ApiScenarioStepRefType.PARTIAL_REF.name())) {
                // 如果是部分引用，blob表保存启用的子步骤ID
                Set<String> enableStepSet = getEnableStepSet(step.getChildren());
                PartialRefStepDetail stepDetail = new PartialRefStepDetail();
                stepDetail.setEnableStepIds(enableStepSet);
                ApiScenarioStepBlob apiScenarioStepBlob = new ApiScenarioStepBlob();
                apiScenarioStepBlob.setId(step.getId());
                apiScenarioStepBlob.setContent(JSON.toJSONString(stepDetail).getBytes());
                apiScenarioStepsDetails.add(apiScenarioStepBlob);
            }
            apiScenarioStepsDetails.addAll(getPartialRefStepDetails(step.getChildren()));
        }
        return apiScenarioStepsDetails;
    }

    /**
     * 获取步骤及子步骤中 enable 的步骤ID
     *
     * @param steps
     * @return
     */
    private Set<String> getEnableStepSet(List<ApiScenarioStepRequest> steps) {
        Set<String> enableSteps = new HashSet<>();
        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptySet();
        }
        for (ApiScenarioStepRequest step : steps) {
            if (BooleanUtils.isTrue(step.getEnable())) {
                enableSteps.add(step.getId());
            }
            // 完全引用和部分引用不解析子步骤
            if (!isRef(step.getRefType())) {
                // 获取子步骤中 enable 的步骤
                enableSteps.addAll(getEnableStepSet(step.getChildren()));
            }
        }
        return enableSteps;
    }

    private static ApiFileResourceUpdateRequest getApiFileResourceUpdateRequest(String sourceId, String projectId, String operator) {
        String apiScenarioDir = DefaultRepositoryDir.getApiScenarioDir(projectId, sourceId);
        ApiFileResourceUpdateRequest resourceUpdateRequest = new ApiFileResourceUpdateRequest();
        resourceUpdateRequest.setProjectId(projectId);
        resourceUpdateRequest.setFolder(apiScenarioDir);
        resourceUpdateRequest.setResourceId(sourceId);
        resourceUpdateRequest.setApiResourceType(ApiResourceType.API_SCENARIO);
        resourceUpdateRequest.setOperator(operator);
        resourceUpdateRequest.setLogModule(OperationLogModule.API_SCENARIO);
        resourceUpdateRequest.setFileAssociationSourceType(FileAssociationSourceUtil.SOURCE_TYPE_API_DEBUG);
        return resourceUpdateRequest;
    }

    public long getNextNum(String projectId) {
        return NumGenerator.nextNum(projectId, ApplicationNumScope.API_SCENARIO);
    }

    public Long getNextOrder(String projectId) {
        return projectService.getNextOrder(extApiScenarioMapper::getLastPos, projectId);
    }

    public void delete(String id) {
        checkResourceExist(id);
        apiScenarioMapper.deleteByPrimaryKey(id);
        apiScenarioBlobMapper.deleteByPrimaryKey(id);
        deleteStepByScenarioId(id);
        deleteStepDetailByScenarioId(id);
    }

    public void deleteToGc(String id) {
        checkResourceExist(id);
        ApiScenario apiScenario = new ApiScenario();
        apiScenario.setId(id);
        apiScenario.setDeleted(true);
        apiScenarioMapper.updateByPrimaryKeySelective(apiScenario);
    }

    private void checkAddExist(ApiScenarioAddRequest apiScenario) {
        ApiScenarioExample example = new ApiScenarioExample();
        // 统一模块下名称不能重复
        example.createCriteria()
                .andNameEqualTo(apiScenario.getName())
                .andModuleIdEqualTo(apiScenario.getModuleId());
        if (apiScenarioMapper.countByExample(example) > 0) {
            throw new MSException(API_SCENARIO_EXIST);
        }
    }

    private void checkUpdateExist(ApiScenarioUpdateRequest request) {
        if (StringUtils.isBlank(request.getName())) {
            return;
        }
        // 统一模块下名称不能重复
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria()
                .andIdNotEqualTo(request.getId())
                .andModuleIdEqualTo(request.getModuleId())
                .andNameEqualTo(request.getName());
        if (apiScenarioMapper.countByExample(example) > 0) {
            throw new MSException(API_SCENARIO_EXIST);
        }
    }

    private ApiScenario checkResourceExist(String id) {
        return ServiceUtils.checkResourceExist(apiScenarioMapper.selectByPrimaryKey(id), "permission.system_api_scenario.name");
    }

    public String uploadTempFile(MultipartFile file) {
        return apiFileResourceService.uploadTempFile(file);
    }

    public String debug(ApiScenarioDebugRequest request) {
        ApiScenario apiScenario = apiScenarioMapper.selectByPrimaryKey(request.getId());
        boolean hasSave = apiScenario != null;
        String reportId = IDGenerator.nextStr();

        List<ApiScenarioStepRequest> steps = request.getSteps();

        // 记录引用的资源ID
        Map<String, List<String>> refResourceMap = new HashMap<>();
        buildRefResourceIdMap(steps, refResourceMap);

        // 查询引用的资源详情
        Map<String, String> resourceBlobMap = getResourceBlobMap(refResourceMap);

        // 查询复制的步骤详情
        Map<String, String> detailMap = getStepDetailMap(steps, request.getStepDetails());

        // 解析生成待执行的场景树
        MsScenario msScenario = new MsScenario();
        msScenario.setScenarioConfig(getScenarioConfig(request, hasSave));
        parseStep2MsElement(msScenario, steps, resourceBlobMap, detailMap);

        ApiResourceRunRequest runRequest = BeanUtils.copyBean(new ApiResourceRunRequest(), request);
        runRequest.setProjectId(request.getProjectId());
        runRequest.setTestId(request.getId());
        runRequest.setReportId(reportId);
        runRequest.setResourceType(ApiResourceType.API_SCENARIO.name());
        runRequest.setRunMode(ApiExecuteRunMode.BACKEND_DEBUG.name());
        runRequest.setTempFileIds(request.getTempFileIds());
        runRequest.setGrouped(request.getGrouped());
        runRequest.setEnvironmentId(request.getEnvironmentId());
        runRequest.setTestElement(msScenario);

        apiExecuteService.debug(runRequest);

        return reportId;
    }

    /**
     * 将步骤解析成 MsTestElement 树结构
     *
     * @param parentElement
     * @param steps
     * @param resourceBlobMap
     * @param stepDetailMap
     */
    private void parseStep2MsElement(AbstractMsTestElement parentElement,
                                     List<ApiScenarioStepRequest> steps,
                                     Map<String, String> resourceBlobMap,
                                     Map<String, String> stepDetailMap) {
        if (CollectionUtils.isNotEmpty(steps)) {
            parentElement.setChildren(new LinkedList<>());
        }
        for (ApiScenarioStepRequest step : steps) {
            StepParser stepParser = StepParserFactory.getStepParser(step.getStepType());
            if (stepParser == null || BooleanUtils.isFalse(step.getEnable())) {
                continue;
            }
            if (StringUtils.equals(step.getStepType(), ApiScenarioStepRefType.PARTIAL_REF.name())) {
                setPartialRefStepEnable(step, stepDetailMap);
            }
            // 将步骤详情解析生成对应的MsTestElement
            AbstractMsTestElement msTestElement = stepParser.parse(step, resourceBlobMap.get(step.getResourceId()), stepDetailMap.get(step.getId()));
            if (msTestElement != null) {
                parentElement.getChildren().add(msTestElement);
            }
            if (CollectionUtils.isNotEmpty(step.getChildren())) {
                parseStep2MsElement(msTestElement, step.getChildren(), resourceBlobMap, stepDetailMap);
            }
        }
    }

    /**
     * 设置部分引用的步骤的启用状态
     * @param step
     * @param stepDetailMap
     */
    private void setPartialRefStepEnable(ApiScenarioStepRequest step, Map<String, String> stepDetailMap) {
        String stepDetail = stepDetailMap.get(step.getId());
        if (StringUtils.isBlank(stepDetail)) {
            return;
        }
        PartialRefStepDetail partialRefStepDetail = JSON.parseObject(stepDetail, PartialRefStepDetail.class);
        setChildPartialRefEnable(step.getChildren(), partialRefStepDetail.getEnableStepIds(), stepDetailMap);
    }

    /**
     * 设置部分引用的步骤的启用状态
     * @param steps
     * @param enableStepIds
     * @param stepDetailMap
     */
    private void setChildPartialRefEnable(List<ApiScenarioStepRequest> steps, Set<String> enableStepIds, Map<String, String> stepDetailMap) {
        for (ApiScenarioStepRequest step : steps) {
            if (StringUtils.equals(step.getRefType(), ApiScenarioStepRefType.REF.name())) {
                // 引用的启用不修改
                continue;
            }
            // 非完全引用的步骤，使用当前场景配置的启用状态
            step.setEnable(enableStepIds.contains(step.getId()));
            if (StringUtils.equals(step.getRefType(), ApiScenarioStepRefType.PARTIAL_REF.name())) {
                // 如果是部分引用的场景，重新获取详情再解析
                setPartialRefStepEnable(step, stepDetailMap);
                continue;
            }
            // 非完全引用和部分引用的步骤，递归设置子步骤
            if (CollectionUtils.isNotEmpty(step.getChildren())) {
                setChildPartialRefEnable(step.getChildren(), enableStepIds, stepDetailMap);
            }
        }
    }

    private Map<String, String> getStepDetailMap(List<ApiScenarioStepRequest> steps, Map<String, Object> stepDetailsParam) {
        List<String> needBlobStepIds = new ArrayList<>();
        for (ApiScenarioStepRequest step : steps) {
            if (BooleanUtils.isFalse(step.getEnable())) {
                continue;
            }
            if (StringUtils.equalsAny(step.getStepType(), ApiScenarioStepRefType.REF.name())
                    && !StringUtils.equals(step.getRefType(), ApiScenarioStepType.API.name())) {
                // 非完全引用的步骤和接口定义的步骤，才需要查blob
                continue;
            }
            if (stepDetailsParam != null && stepDetailsParam.keySet().contains(step.getId())) {
                // 前端传了blob，不需要再查
                continue;
            }
            needBlobStepIds.add(step.getId());
        }

        Map<String, String> stepDetails = getStepBlobByIds(needBlobStepIds).stream()
                .collect(Collectors.toMap(ApiScenarioStepBlob::getId, blob -> new String(blob.getContent())));
        // 前端有传，就用前端传的
        if (stepDetailsParam != null) {
            stepDetailsParam.forEach((stepId, detail) -> stepDetails.put(stepId, JSON.toJSONString(detail)));
        }
        return stepDetails;
    }

    private Map<String, String> getResourceBlobMap(Map<String, List<String>> refResourceMap) {
        Map<String, String> resourceBlobMap = new HashMap<>();
        List<String> apiIds = refResourceMap.get(ApiScenarioStepType.API.name());
        List<ApiDefinitionBlob> apiDefinitionBlobs = apiDefinitionService.getBlobByIds(apiIds);
        apiDefinitionBlobs.forEach(blob -> resourceBlobMap.put(blob.getId(), new String(blob.getRequest())));

        List<String> apiCaseIds = refResourceMap.get(ApiScenarioStepType.API_CASE.name());
        List<ApiTestCaseBlob> apiTestCaseBlobs = apiTestCaseService.getBlobByIds(apiCaseIds);
        apiTestCaseBlobs.forEach(blob -> resourceBlobMap.put(blob.getId(), new String(blob.getRequest())));

        List<String> apiScenarioIds = refResourceMap.get(ApiScenarioStepType.API_SCENARIO.name());
        List<ApiScenarioBlob> apiScenarioBlobs = getBlobByIds(apiScenarioIds);
        apiScenarioBlobs.forEach(blob -> resourceBlobMap.put(blob.getId(), new String(blob.getConfig())));
        return resourceBlobMap;
    }

    private List<ApiScenarioStepBlob> getStepBlobByIds(List<String> stepIds) {
        if (CollectionUtils.isEmpty(stepIds)) {
            return Collections.emptyList();
        }
        ApiScenarioStepBlobExample example = new ApiScenarioStepBlobExample();
        example.createCriteria().andIdIn(stepIds);
        return apiScenarioStepBlobMapper.selectByExampleWithBLOBs(example);
    }

    private List<ApiScenarioBlob> getBlobByIds(List<String> apiScenarioIds) {
        if (CollectionUtils.isEmpty(apiScenarioIds)) {
            return Collections.emptyList();
        }
        ApiScenarioBlobExample example = new ApiScenarioBlobExample();
        example.createCriteria().andIdIn(apiScenarioIds);
        return apiScenarioBlobMapper.selectByExampleWithBLOBs(example);
    }

    private void buildRefResourceIdMap(List<ApiScenarioStepRequest> steps, Map<String, List<String>> refResourceIdMap) {
        for (ApiScenarioStepRequest step : steps) {
            if (isRef(step.getRefType()) && BooleanUtils.isTrue(step.getEnable())) {
                // 记录引用的步骤ID
                List<String> resourceIds = refResourceIdMap.get(step.getStepType());
                if (resourceIds == null) {
                    resourceIds = new ArrayList<>();
                    refResourceIdMap.put(step.getStepType(), resourceIds);
                }
                resourceIds.add(step.getResourceId());
            }

            if (CollectionUtils.isNotEmpty(step.getChildren())) {
                buildRefResourceIdMap(step.getChildren(), refResourceIdMap);
            }
        }
    }

    private ScenarioConfig getScenarioConfig(ApiScenarioDebugRequest request, boolean hasSave) {
        ScenarioConfig scenarioConfig = null;
        if (request.getScenarioConfig() != null) {
            // 优先使用前端传的配置
            scenarioConfig = request.getScenarioConfig();
        } else if (hasSave) {
            // 没传并且保存过，则从数据库获取
            ApiScenarioBlob apiScenarioBlob = apiScenarioBlobMapper.selectByPrimaryKey(request.getId());
            if (apiScenarioBlob != null) {
                scenarioConfig = JSON.parseObject(new String(apiScenarioBlob.getConfig()), ScenarioConfig.class);
            }
        }
        return scenarioConfig;
    }
}
import MSR from '@/api/http/index';
import {
  GetEnvironmentUrl,
  GetEnvListUrl,
  GetPluginOptionsUrl,
  GetPluginScriptUrl,
  GetProtocolListUrl,
  LocalExecuteApiDebugUrl,
} from '@/api/requrls/api-test/common';

import {
  ExecuteRequestParams,
  GetPluginOptionsParams,
  PluginConfig,
  PluginOption,
  ProtocolItem,
} from '@/models/apiTest/common';
import { EnvConfig, EnvironmentItem } from '@/models/projectManagement/environmental';

// 获取协议列表
export function getProtocolList(organizationId: string) {
  return MSR.get<ProtocolItem[]>({ url: GetProtocolListUrl, params: organizationId });
}

// 获取插件表单选项
export function getPluginOptions(data: GetPluginOptionsParams) {
  return MSR.get<PluginOption[]>({ url: GetPluginOptionsUrl, data });
}

// 获取插件配置
export function getPluginScript(pluginId: string) {
  return MSR.get<PluginConfig>({ url: GetPluginScriptUrl, params: pluginId });
}

// 本地执行调试
export function localExecuteApiDebug(host: string, data: ExecuteRequestParams) {
  return MSR.post<ExecuteRequestParams>({ url: `${host}${LocalExecuteApiDebugUrl}`, data });
}

// 获取环境列表
export function getEnvList(projectId: string) {
  return MSR.get<EnvironmentItem[]>({ url: GetEnvListUrl, params: projectId });
}

// 获取环境详情
export function getEnvironment(envId: string) {
  return MSR.get<EnvConfig>({ url: GetEnvironmentUrl, params: envId });
}

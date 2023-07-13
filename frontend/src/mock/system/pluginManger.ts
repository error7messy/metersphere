import Mock from 'mockjs';
import setupMock, { successTableResponseWrap } from '@/utils/setup-mock';

const getPluginList = () => {
  return [
    {
      id: '1-1',
      name: '插件一',
      describe: '插件一',
      enable: true,
      createTime: 'number',
      updateTime: 'number',
      jarPackage: 'string',
      version: 'string',
      applicationScene: 'string',
      createUser: 'string',
      updateUser: 'string',
      organizationList: [
        {
          id: 'string',
          num: 0,
          name: '组织 1',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 2',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 3',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 4',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
      ],
      steps: [
        {
          id: '1-1-1',
          name: '步骤一',
        },
        {
          id: '1-1-2',
          name: '步骤二',
        },
        {
          id: '1-1-3',
          name: '步骤三',
        },
      ],
    },
    {
      id: '2-1',
      name: '插件一',
      describe: '插件一',
      enable: true,
      createTime: 'number',
      updateTime: 'number',
      jarPackage: 'string',
      version: 'string',
      applicationScene: 'string',
      createUser: 'string',
      updateUser: 'string',
      organizationList: [
        {
          id: 'string',
          num: 0,
          name: '组织 1',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 2',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 3',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
        {
          id: 'string',
          num: 0,
          name: '组织 4',
          description: 'blabla',
          createTime: 0,
          updateTime: 0,
          createUser: 'string',
          updateUser: 'string',
          deleted: true,
          deleteUser: 'string',
          deleteTime: 0,
          enable: true,
        },
      ],
      steps: [
        {
          id: '2-1-1',
          name: '步骤2-一',
        },
        {
          id: '2-1-2',
          name: '步骤2-二',
        },
        {
          id: '2-1-3',
          name: '步骤3-三',
        },
      ],
    },
  ];
};

setupMock({
  setup: () => {
    Mock.mock(new RegExp('/plugin/page'), () => {
      return successTableResponseWrap(getPluginList());
    });
  },
});

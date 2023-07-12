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
      children: [
        {
          id: '1-1-1',
          name: '插件1-1',
          describe: '插件1-1',
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
      children: [
        {
          id: '2-1-1',
          name: '插件2-1',
          describe: '插件2-1',
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
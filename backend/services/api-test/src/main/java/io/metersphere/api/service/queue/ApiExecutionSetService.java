package io.metersphere.api.service.queue;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiExecutionSetService {

    public static final String SET_PREFIX = "set:";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 初始化执行集合
     * 保存需要执行的资源ID
     * @param setId
     * @param resourceIds
     */
    public void initSet(String setId, List<String> resourceIds) {
        resourceIds.forEach(resourceId -> redisTemplate.opsForSet().add(SET_PREFIX + setId, resourceId));
    }

    /**
     * 从执行集合中去除选项
     */
    public void removeItem(String setId, String resourceId) {
        redisTemplate.opsForSet().remove(SET_PREFIX + setId, resourceId);
        Long size = redisTemplate.opsForSet().size(SET_PREFIX + setId);
        if (size == null || size == 0) {
            // 集合没有元素，则删除集合
            redisTemplate.delete(SET_PREFIX + setId);
        }
    }
}

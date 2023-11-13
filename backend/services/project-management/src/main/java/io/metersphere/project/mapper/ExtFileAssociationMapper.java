package io.metersphere.project.mapper;

import io.metersphere.project.dto.filemanagement.FileAssociationSource;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 注：本类中所有带有querySql参数的方法，不能直接传入sql，
 *  要使用FileAssociationResourceUtil.getQuerySql(sourceType)。防止SQL注入
 */
public interface ExtFileAssociationMapper {
    FileAssociationSource selectNameBySourceTableAndId(@Param("querySql") String querySql, @Param("sourceId") String sourceId);
    List<FileAssociationSource> selectAssociationSourceBySourceTableAndIdList(@Param("querySql") String querySql, @Param("idList") List<String> sourceIdList);
}

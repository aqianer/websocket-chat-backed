package com.example.websocketchatbacked.converter;

import com.example.websocketchatbacked.entity.KbChunk;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

// 声明为MapStruct映射器，componentModel="spring"支持Spring注入
@Mapper(componentModel = "spring")
public interface GlobalConverter {

    // 单例实例
    GlobalConverter INSTANCE = Mappers.getMapper(GlobalConverter.class);

//    // ====================== 场景1：String → KBChunk ======================
//    // 单个String转KBChunk
//    @Mapping(target = "content", source = "source") // source是入参String，映射到KBChunk的content字段
//    KbChunk stringToKBChunk(String source);
//
//    // 在GlobalConverter接口中新增：List<String>合并为一个KBChunk
//    default List<KbChunk> stringListToKBChunk(List<String> sourceList) {
//        // 自定义合并逻辑：用逗号拼接所有字符串作为content
//        String mergedContent = String.join(",", sourceList);
//        // 调用已有的单个转换方法
//        return stringToKBChunk(mergedContent);
//    }

//    // 集合转换：自动循环调用stringToKBChunk
//    List<KBChunk> stringListToKBChunkList(List<String> sourceList);
//
//    // ====================== 场景2：Entity → DTO ======================
//    // 单个UserEntity转UserDTO
//    @Mapping(target = "userName", source = "name") // 字段名不同：Entity的name → DTO的userName
//    @Mapping(target = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss") // 日期格式化
//    UserDTO userEntityToDTO(UserEntity entity);
//
//    // 集合转换：自动循环调用userEntityToDTO
//    List<UserDTO> userEntityListToDTOList(List<UserEntity> entityList);
//
//    // ====================== 场景3：多对象合并 ======================
//    // 合并User和Order到OrderDetailDTO
//    @Mapping(target = "userId", source = "user.id") // 嵌套属性：user的id → DTO的userId
//    @Mapping(target = "orderNo", source = "order.orderNumber") // order的orderNumber → DTO的orderNo
//    @Mapping(target = "userName", source = "user.name")
//    OrderDetailDTO mergeUserAndOrderToDTO(User user, Order order);
//
//    // ====================== 场景4：更新现有对象 ======================
//    // 用UserUpdateDTO更新UserEntity（只更新非null字段）
//    @Mapping(target = "id", ignore = true) // 忽略id，不更新
//    void updateUserFromDTO(UserUpdateDTO dto, @MappingTarget UserEntity entity);

}

package cn.wth.ai.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 5th
 * @Description: Tika返回对象
 * @CreateTime: 2025-04-09 13:47
 */

@Accessors(chain = true)
@Data
public class TikaVO implements Serializable {

    private List<String> text;

    private List<String> metadata;

}

package edu.qhjy.score_service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

/**
 * DBF文件导入请求数据传输对象
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Data
@Accessors(chain = true)
public class DbfImportRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * DBF文件
     */
    @NotNull(message = "DBF文件不能为空")
    private MultipartFile file;

    /**
     * 考试计划代码
     */
    @NotBlank(message = "考试计划代码不能为空")
    private String ksjhdm;

    /**
     * 验证文件是否为DBF格式
     */
    public boolean isValidDbfFile() {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        return originalFilename.toLowerCase().endsWith(".dbf");
    }

    /**
     * 验证文件大小是否在限制范围内（300MB）
     */
    public boolean isValidFileSize() {
        if (file == null) {
            return false;
        }

        // 300MB = 300 * 1024 * 1024 bytes
        long maxSize = 300L * 1024 * 1024;
        return file.getSize() <= maxSize;
    }

    /**
     * 获取文件名（不含路径）
     */
    public String getFileName() {
        if (file == null) {
            return null;
        }
        return file.getOriginalFilename();
    }

    /**
     * 获取文件大小（字节）
     */
    public long getFileSize() {
        if (file == null) {
            return 0;
        }
        return file.getSize();
    }
}
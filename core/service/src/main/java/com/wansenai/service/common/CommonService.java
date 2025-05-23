/*
 * Copyright 2023-2025 EAIRP Team, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://opensource.wansenai.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.wansenai.service.common;

import com.wansenai.bo.FileDataBO;
import com.wansenai.utils.response.Response;
import com.wansenai.vo.CaptchaVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface CommonService {
    CaptchaVO getCaptcha();
    Boolean sendSmsCode(Integer type, String phoneNumber);

    Response<String> sendEmailCode(Integer type, String email);

    Response<String> uploadExclsData(MultipartFile file);

    Response<String> productCoverUpload(MultipartFile file, Integer type);

    Response<List<String>> uploadOss(List<MultipartFile> files);

    Response<String> generateSnowflakeId(String type);

    List<FileDataBO> getFileList(String fileId);

    String getProductName(Long productId);

    String getProductCategoryName(Long productCategoryId);

    String getWarehouseName(Long warehouseId);

    String getMemberName(Long memberId);

    String getSupplierName(Long supplierId);

    String getCustomerName(Long customerId);

    String getOperatorName(Long operatorId);

    String getRelatedPersonName(Long relatedPersonId);

    String getAccountName(Long accountId);
}

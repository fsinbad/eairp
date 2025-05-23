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
package com.wansenai.dto.warehouse;

import com.wansenai.bo.AllotStockBO;
import com.wansenai.bo.FileDataBO;
import lombok.Data;

import java.util.List;

@Data
public class AllotReceiptDTO {

    private Long id;

    private String receiptNumber;

    private String receiptDate;

    private String remark;

    private Integer status;

    private List<AllotStockBO> tableData;

    private List<FileDataBO> files;
}

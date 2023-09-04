package com.wansensoft.api.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wansensoft.entities.tenant.Tenant;
import com.wansensoft.entities.user.User;
import com.wansensoft.entities.user.UserEx;
import com.wansensoft.service.role.RoleService;
import com.wansensoft.service.tenant.TenantService;
import com.wansensoft.service.user.UserService;
import com.wansensoft.utils.constants.ExceptionConstants;
import com.wansensoft.plugins.exception.BusinessParamCheckingException;
import com.wansensoft.service.redis.RedisService;
import com.wansensoft.utils.*;
import com.wansensoft.vo.TreeNodeEx;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping(value = "/user")
@Api(tags = {"用户管理"})
public class UserController {
    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${manage.roleId}")
    private Integer manageRoleId;

    private final UserService userService;

    private final RoleService roleService;

    private final TenantService tenantService;

    private final RedisService redisService;

    private static String SUCCESS = "操作成功";
    private static String ERROR = "操作失败";

    public UserController(UserService userService, RoleService roleService, TenantService tenantService, RedisService redisService) {
        this.userService = userService;
        this.roleService = roleService;
        this.tenantService = tenantService;
        this.redisService = redisService;
    }


    @PostMapping(value = "/login")
    @ApiOperation(value = "登录")
    public BaseResponseInfo login(@RequestBody User userParam,
                                  HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = userService.login(userParam, request);
            res.code = 200;
            res.data = data;
        } catch(Exception e){
            logger.error(e.getMessage());
            res.code = 500;
            res.data = "用户登录失败";
        }
        return res;
    }

    @PostMapping(value = "/weixinLogin")
    @ApiOperation(value = "微信登录")
    public BaseResponseInfo weixinLogin(@RequestBody JSONObject jsonObject,
                                  HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String weixinCode = jsonObject.getString("weixinCode");
            User user = userService.getUserByWeixinCode(weixinCode);
            if(user == null) {
                res.code = 501;
                res.data = "微信未绑定";
            } else {
                Map<String, Object> data = userService.login(user, request);
                res.code = 200;
                res.data = data;
            }
        } catch(Exception e){
            logger.error(e.getMessage());
            res.code = 500;
            res.data = "用户登录失败";
        }
        return res;
    }

    @PostMapping(value = "/weixinBind")
    @ApiOperation(value = "绑定微信")
    public Response weixinBind(@RequestBody JSONObject jsonObject) {
        String loginName = jsonObject.getString("loginName");
        String password = jsonObject.getString("password");
        String weixinCode = jsonObject.getString("weixinCode");
        int res = userService.weixinBind(loginName, password, weixinCode);
        if(res > 0) {
            return Response.responseMsg(ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return Response.responseMsg(ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    @GetMapping(value = "/getUserSession")
    @ApiOperation(value = "获取用户信息")
    public BaseResponseInfo getSessionUser(HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<>();
            Long userId = Long.parseLong(redisService.getObjectFromSessionByKey(request,"userId").toString());
            User user = userService.getUser(userId);
            user.setPassword(null);
            data.put("user", user);
            res.code = 200;
            res.data = data;
        } catch(Exception e){
            res.code = 500;
            res.data = "获取session失败";
        }
        return res;
    }

    @GetMapping(value = "/logout")
    @ApiOperation(value = "退出")
    public BaseResponseInfo logout(HttpServletRequest request, HttpServletResponse response)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            redisService.deleteObjectBySession(request,"userId");
            redisService.deleteObjectBySession(request,"roleType");
            redisService.deleteObjectBySession(request,"clientIp");
        } catch(Exception e){
            res.code = 500;
            res.data = "退出失败";
        }
        return res;
    }

    @PostMapping(value = "/resetPwd")
    @ApiOperation(value = "重置密码")
    public Response resetPwd(@RequestBody JSONObject jsonObject,
                                     HttpServletRequest request) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        Long id = jsonObject.getLong("id");
        String password = "123456";
        String md5Pwd = Tools.md5Encryp(password);
        int update = userService.resetPwd(md5Pwd, id);
        if(update > 0) {
            return Response.responseMsg(ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return Response.responseMsg(ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    @PutMapping(value = "/updatePwd")
    @ApiOperation(value = "更新密码")
    public Response updatePwd(@RequestBody JSONObject jsonObject, HttpServletRequest request)throws Exception {
        Integer flag = 0;
        Map<String, Object> objectMap = new HashMap<String, Object>();
        try {
            String info = "";
            Long userId = jsonObject.getLong("userId");
            String oldpwd = jsonObject.getString("oldpassword");
            String password = jsonObject.getString("password");
            User user = userService.getUser(userId);
            //必须和原始密码一致才可以更新密码
            if (oldpwd.equalsIgnoreCase(user.getPassword())) {
                user.setPassword(password);
                flag = userService.updateUserByObj(user); //1-成功
                info = "修改成功";
            } else {
                flag = 2; //原始密码输入错误
                info = "原始密码输入错误";
            }
            objectMap.put("status", flag);
            if(flag > 0) {
                return Response.responseData(info);
            } else {
                return Response.responseMsg(ErpInfo.ERROR.name, ErpInfo.ERROR.code);
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>>>>>>修改用户ID为 ： " + jsonObject.getLong("userId") + "密码信息失败", e);
            flag = 3;
            objectMap.put("status", flag);
            return Response.responseMsg(ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 获取全部用户数据列表
     * @param request
     * @return
     */
    @GetMapping(value = "/getAllList")
    @ApiOperation(value = "获取全部用户数据列表")
    public BaseResponseInfo getAllList(HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<String, Object>();
            List<User> dataList = userService.getUser();
            if(dataList!=null) {
                data.put("userList", dataList);
            }
            res.code = 200;
            res.data = data;
        } catch(Exception e){
            res.code = 500;
            res.data = "获取失败";
        }
        return res;
    }

    /**
     * 用户列表，用于用户下拉框
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/getUserList")
    @ApiOperation(value = "用户列表")
    public JSONArray getUserList(HttpServletRequest request)throws Exception {
        JSONArray dataArray = new JSONArray();
        try {
            List<User> dataList = userService.getUser();
            if (null != dataList) {
                for (User user : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("id", user.getId());
                    item.put("userName", user.getUsername());
                    dataArray.add(item);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return dataArray;
    }

    /**
     * description:
     *  新增用户及机构和用户关系
     * @Param: beanJson
     * @return java.lang.Object
     */
    @PostMapping("/addUser")
    @ApiOperation(value = "新增用户")
    @ResponseBody
    public Object addUser(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        User userInfo = userService.getCurrentUser();
        Tenant tenant = tenantService.getTenantByTenantId(userInfo.getTenantId());
        Long count = userService.countUser(null,null);
        if(tenant!=null) {
            if(count>= tenant.getUserNumLimit()) {
                throw new BusinessParamCheckingException(ExceptionConstants.USER_OVER_LIMIT_FAILED_CODE,
                        ExceptionConstants.USER_OVER_LIMIT_FAILED_MSG);
            } else {
                UserEx ue= JSONObject.parseObject(obj.toJSONString(), UserEx.class);
                userService.addUserAndOrgUserRel(ue, request);
            }
        }
        return result;
    }

    /**
     * description:
     *  修改用户及机构和用户关系
     * @Param: beanJson
     * @return java.lang.Object
     */
    @PutMapping("/updateUser")
    @ApiOperation(value = "修改用户")
    @ResponseBody
    public Object updateUser(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        UserEx ue= JSONObject.parseObject(obj.toJSONString(), UserEx.class);
        userService.updateUserAndOrgUserRel(ue, request);
        return result;
    }

    /**
     * 注册用户
     * @param ue
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/registerUser")
    @ApiOperation(value = "注册用户")
    public Object registerUser(@RequestBody UserEx ue,
                               HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        ue.setUsername(ue.getLoginName());
        userService.checkLoginName(ue); //检查登录名
        ue = userService.registerUser(ue,manageRoleId,request);
        return result;
    }

    /**
     * 获取机构用户树
     * @return
     * @throws Exception
     */
    @RequestMapping("/getOrganizationUserTree")
    @ApiOperation(value = "获取机构用户树")
    public JSONArray getOrganizationUserTree()throws Exception{
        JSONArray arr=new JSONArray();
        List<TreeNodeEx> organizationUserTree= userService.getOrganizationUserTree();
        if(organizationUserTree!=null&&organizationUserTree.size()>0){
            for(TreeNodeEx node:organizationUserTree){
                String str=JSON.toJSONString(node);
                JSONObject obj=JSON.parseObject(str);
                arr.add(obj) ;
            }
        }
        return arr;
    }

    @GetMapping(value = "/getCurrentPriceLimit")
    @ApiOperation(value = "查询当前用户的价格屏蔽")
    public BaseResponseInfo getCurrentPriceLimit(HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<>();
            String priceLimit = roleService.getCurrentPriceLimit(request);
            data.put("priceLimit", priceLimit);
            res.code = 200;
            res.data = data;
        } catch(Exception e){
            e.printStackTrace();
            res.code = 500;
            res.data = "获取session失败";
        }
        return res;
    }

    /**
     * 获取当前用户的角色类型
     * @param request
     * @return
     */
    @GetMapping("/getRoleTypeByCurrentUser")
    @ApiOperation(value = "获取当前用户的角色类型")
    public BaseResponseInfo getRoleTypeByCurrentUser(HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<String, Object>();
            String roleType = redisService.getObjectFromSessionByKey(request,"roleType").toString();
            data.put("roleType", roleType);
            res.code = 200;
            res.data = data;
        } catch(Exception e){
            e.printStackTrace();
            res.code = 500;
            res.data = "获取失败";
        }
        return res;
    }

    /**
     * 获取随机校验码
     * @param response
     * @param key
     * @return
     */
    @GetMapping(value = "/randomImage/{key}")
    @ApiOperation(value = "获取随机校验码")
    public BaseResponseInfo randomImage(HttpServletResponse response,@PathVariable String key){
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<>();
            String codeNum = Tools.getCharAndNum(4);
            String base64 = RandImageUtil.generate(codeNum);
            data.put("codeNum", codeNum);
            data.put("base64", base64);
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            e.printStackTrace();
            res.code = 500;
            res.data = "获取失败";
        }
        return res;
    }

    /**
     * 批量设置状态-启用或者禁用
     * @param jsonObject
     * @param request
     * @return
     */
    @PostMapping(value = "/batchSetStatus")
    @ApiOperation(value = "批量设置状态")
    public Response batchSetStatus(@RequestBody JSONObject jsonObject,
                                 HttpServletRequest request)throws Exception {
        Byte status = jsonObject.getByte("status");
        String ids = jsonObject.getString("ids");
        int res = userService.batchSetStatus(status, ids, request);
        if(res > 0) {
            return Response.responseMsg(ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return Response.responseMsg(ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 获取当前用户的用户数量和租户信息
     * @param request
     * @return
     */
    @GetMapping(value = "/infoWithTenant")
    @ApiOperation(value = "获取当前用户的用户数量和租户信息")
    public BaseResponseInfo randomImage(HttpServletRequest request){
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            Map<String, Object> data = new HashMap<>();
            Long userId = Long.parseLong(redisService.getObjectFromSessionByKey(request,"userId").toString());
            User user = userService.getUser(userId);
            //获取当前用户数
            int userCurrentNum = userService.getUser().size();
            Tenant tenant = tenantService.getTenantByTenantId(user.getTenantId());
            data.put("type", tenant.getType()); //租户类型，0免费租户，1付费租户
            data.put("expireTime", Tools.parseDateToStr(tenant.getExpireTime()));
            data.put("userCurrentNum", userCurrentNum);
            data.put("userNumLimit", tenant.getUserNumLimit());
            data.put("tenantId", tenant.getTenantId());
            res.code = 200;
            res.data = data;
        } catch (Exception e) {
            e.printStackTrace();
            res.code = 500;
            res.data = "获取失败";
        }
        return res;
    }
}

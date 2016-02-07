/*
 * Copyright (c) 2015 AppsoFluna.
 * All rights reserved.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.appsofluna.simpleapps.service;

import com.appsofluna.simpleapps.model.App;
import com.appsofluna.simpleapps.model.Field;
import com.appsofluna.simpleapps.model.Item;
import com.appsofluna.simpleapps.model.Permission;
import com.appsofluna.simpleapps.model.Role;
import com.appsofluna.simpleapps.repository.AppRepository;
import com.appsofluna.simpleapps.repository.FieldRepository;
import com.appsofluna.simpleapps.repository.ItemRepository;
import com.appsofluna.simpleapps.repository.PermissionRepository;
import com.appsofluna.simpleapps.repository.RoleRepository;
import com.appsofluna.simpleapps.util.JsonUtil;
import com.appsofluna.simpleapps.util.SAConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Charaka Gunatillake <charakajg[at]gmail[dot]com>
 */
@Service
public class AppService {
    private static final Logger logger = LoggerFactory.getLogger(AppService.class);
    
    @Autowired
    private AppRepository appRepo;
    @Autowired
    private ItemRepository itemRepo;
    @Autowired
    private FieldRepository fieldRepo;
    @Autowired
    private RoleRepository roleRepo;
    @Autowired
    private PermissionRepository permissionRepo;
    
    public Map getAppForCodeGeneration(long id) {
        Map map = new HashMap();
        App app = appRepo.findOne(id);
        Map appMap = new HashMap();
        appMap.put("name", app.getName());
        appMap.put("id", app.getId());
        
        List itemsSetList = new ArrayList();
        List<Item> itemList = itemRepo.findByApp(id);
        for (Item item: itemList) {
            Map itemMap = new HashMap();
            Long itemId = item.getId();
            itemMap.put("label", item.getLabel());
            itemMap.put("name", clearName(item.getName()));
            itemMap.put("id", itemId);
            List fieldSetList = new ArrayList();
            List<Field> fieldList = fieldRepo.findByItem(itemId);
            for (Field field: fieldList) {
                Map fieldMap = new HashMap();
                Long fieldId = field.getId();
                fieldMap.put("label",field.getLabel());
                fieldMap.put("name",clearName(field.getName()));
                fieldMap.put("type",field.getType());
                fieldMap.put("id",fieldId);
                fieldMap.put("extra",getFieldExtra(field));
                fieldSetList.add(fieldMap);
            }
            itemMap.put("fields",fieldSetList);
            itemsSetList.add(itemMap);
        }
        appMap.put("items", itemsSetList);
        
        List roleSetList = new ArrayList();
        List<Role> roleList = roleRepo.findByApp(id);
        boolean administratorFound = false;
        for (Role role: roleList) {
            Map roleMap = new HashMap();
            Long roleId = role.getId();
            String roleName = role.getName();
            if (SAConstraints.DEFAULT_ROLENAME.equals(roleName)) administratorFound = true;
            roleMap.put("name", roleName);
            roleMap.put("id", roleId);
            boolean addAll = role.isAllItemsAllowed();
            List<String> allowedItems = new ArrayList();
            List<String> creatableItems = new ArrayList();
            List<String> editableItems = new ArrayList();
            List<String> deletableItems = new ArrayList();
            for (Item item: itemList) {
                String name = item.getName().toLowerCase();
                if (addAll) {
                    allowedItems.add(name);
                    creatableItems.add(name);
                    editableItems.add(name);
                    deletableItems.add(name);
                } else {
                    Permission permission = permissionRepo.findByRoleAndItem(roleId, item.getId());
                    if (permission!=null) {
                        if (permission.isAccessAllowed()) allowedItems.add(name);
                        if (permission.isCreateAllowed()) creatableItems.add(name);
                        if (permission.isEditAllowed()) editableItems.add(name);
                        if (permission.isDeleteAllowed()) deletableItems.add(name);
                    }
                }
            }
            roleMap.put("allowed_items", allowedItems);
            roleMap.put("creatable_items", creatableItems);
            roleMap.put("editable_items", editableItems);
            roleMap.put("deletable_items", deletableItems);
            roleSetList.add(roleMap);
        }
        if (!administratorFound) {
            Map roleMap = new HashMap();
            roleMap.put("name", SAConstraints.DEFAULT_ROLENAME);
            roleMap.put("id", -1);
            List<String> allowedItems = new ArrayList();
            List<String> creatableItems = new ArrayList();
            List<String> editableItems = new ArrayList();
            List<String> deletableItems = new ArrayList();
            for (Item item: itemList) {
                String name = item.getName().toLowerCase();
                allowedItems.add(name);
                creatableItems.add(name);
                editableItems.add(name);
                deletableItems.add(name);
            }
            roleMap.put("allowed_items", allowedItems);
            roleMap.put("creatable_items", creatableItems);
            roleMap.put("editable_items", editableItems);
            roleMap.put("deletable_items", deletableItems);
            roleSetList.add(roleMap);
        }
        appMap.put("roles", roleSetList);
        
        map.put("app",appMap);
        return map;
    }
    
    private String clearName(String name) {
        return (name == null) ? "" : name.toLowerCase().replaceAll(" ", "_");
    }

    private Map getFieldExtra(Field field) {
        Map map = new HashMap();
        if (SAConstraints.FIELD_TYPE_ITEM.equals(field.getType())) {
            String format = field.getFormat();
            Map<String, Object> formatMap = JsonUtil.stringToMap(format);
            Object refItemIdObj = formatMap.get(SAConstraints.FIELD_TYPE_ITEM_PARM_REFER);
            Long refItemId;
            if (refItemIdObj instanceof Long) {
                refItemId = (Long) refItemIdObj;
            } else if (refItemIdObj instanceof String) {
                refItemId = Long.parseLong((String)refItemIdObj);
            } else {
                logger.error("unable to format item because of ref id");
                return null;
            }
            Item refItem = itemRepo.findOne(refItemId);
            map.put("refItem", clearName(refItem.getName()));
            logger.info("refItem: {}",clearName(refItem.getName()));
            List<String> refFieldList = new ArrayList();
            Object templateObj = formatMap.get(SAConstraints.FIELD_TYPE_ITEM_PARM_TEMPLATE);
            String template = null;
            if (templateObj instanceof String) {
                template = (String)templateObj;
                logger.info("uses field template: {}",template);
            }
            if (template==null || template.trim().equals("")) {
                template = refItem.getTemplate();
                logger.info("uses item template: {}",template);
            }
            if (template!=null && !template.trim().equals("")) {
                logger.info("template works");
                List<Field> refItemFields = fieldRepo.findByItem(refItemId);
                Map<String,Field> refItemFieldMap = new HashMap<>();
                for (Field refField: refItemFields) {
                    refItemFieldMap.put(refField.getName(),refField);
                }
                Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
                Matcher matcher = pattern.matcher(template);
                while(matcher.find()) {
                    String fieldName = matcher.group(1);
                    if (refItemFieldMap.containsKey(fieldName)) {
                        Field refField = refItemFieldMap.get(fieldName);
                        if (SAConstraints.FIELD_TYPE_ITEM.equalsIgnoreCase(refField.getType())) {
                            refFieldList.add(clearName(fieldName)+"_id");
                        } else {
                            refFieldList.add(clearName(fieldName));
                        }
                    }
                }
            }
            if (refFieldList.isEmpty()) {
                logger.info("item template hadn't work");
                refFieldList.add("id");
            }
            map.put("refFields", refFieldList);
        }
        return map;
    }
}

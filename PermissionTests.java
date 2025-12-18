package com.ascent_project.RBAC;

import com.ascent_project.RBAC.controller.PermissionController;
import com.ascent_project.RBAC.controller.UserRoleController;
import com.ascent_project.RBAC.model.Permission;
import com.ascent_project.RBAC.service.UserRoleService;
import com.sun.istack.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.InputStream;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PermissionTests
{

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String getRootUrl()
    {
        return "http://localhost:" + port;
    }

    private InputStream is;
    private MockMvc mockMvc;

    @Spy
    @InjectMocks
    private PermissionController permissionController = new PermissionController();

    @Before
    public void init()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController).build();
        is = permissionController.getClass().getClassLoader().getResourceAsStream("sample.csv");
    }


    @AfterEach
    public void removeRecords()
    {
        Permission[] permissions= getPermissions(getStringHttpPermission());
        for (Permission permission : permissions) {
            delete(permission.getId());
        }
    }

    @NotNull
    private Permission getPermission(String code, String name, String description, String valid_Until)
    {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        permission.setValid_Until(valid_Until);
        return permission;
    }

    private Permission addPermissionAPI(Permission permission)
    {
        HttpEntity<Permission> entity = getPermissionHttpEntity(permission);
        ResponseEntity<Permission> result=restTemplate.postForEntity(getRootUrl() +
                "/permission/api/v1/managedEntityCode/entityCode1", entity, Permission.class);
        Assert.assertEquals(201,result.getStatusCodeValue());
        Permission permission1 = result.getBody();
        return permission1;
    }

    private Permission[] getPermissions(HttpEntity<String> entity)
    {
        ResponseEntity<Permission[]> response = restTemplate.exchange(getRootUrl() +
                        "/permission/api/v1", HttpMethod.GET, entity, Permission[].class);
        return response.getBody();
    }

    @NotNull
    private HttpEntity<String> getStringHttpPermission()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.add("powerpayTenantId", "permission_test");
        return new HttpEntity<String>(null, headers);
    }

    @NotNull
    private HttpEntity<Permission> getPermissionHttpEntity(Permission permission)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.add("powerpayTenantId", "permission_test");
        return new HttpEntity<>(permission, headers);
    }

    @Test
    public void testSavePermission() throws RestClientException
    {
        Permission permission = getPermission("code1", "name1", "Ready and Waiting", "23-04-2004");
        addPermissionAPI(permission);
    }

    @Test
    public void testUploadFile() throws Exception
    {
        //TODO Fix the before Issue
        init();
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "sample.csv", "multipart/form-data", is);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart(getRootUrl() + "/permission/api/v1/managedEntity/permission").
                file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)).andExpect(MockMvcResultMatchers.status().is(200)).andReturn();
        Assert.assertEquals(200, ((MvcResult) result).getResponse().getStatus());
        Assert.assertNotNull(result.getResponse().getContentAsString());
        Assert.assertEquals("sample.csv", result.getResponse().getContentAsString());
    }
    
    @Test
    public void testGetPermissionById() throws Exception
    {
       Permission permission = getPermission("code2", "name2",
                "Ready and Waiting", "23-04-2004");
        permission = addPermissionAPI(permission);
        String id = permission.getId();

        permission = restTemplate.getForObject(getRootUrl() + "/permission/api/v1/permissionId/" +
                        id, Permission.class);
        Assert.assertNotNull(permission);
        Assert.assertEquals("code2", permission.getCode());
        Assert.assertEquals("name2", permission.getName());
        Assert.assertEquals("Ready and Waiting", permission.getDescription());
        Assert.assertEquals("23-04-2004", permission.getValid_Until());
    }

    @Test
    public void testGetPermissionByCode()
    {
        Permission permission = getPermission("code3", "name3", "Waiting",
                "23-04-2022");
        permission = addPermissionAPI(permission);
        String code = permission.getCode();

        permission = restTemplate.getForObject(getRootUrl() + "/permission/api/v1/permissionCode/"
                + code, Permission.class);
        Assert.assertNotNull(permission);
        Assert.assertEquals("code3", permission.getCode());
        Assert.assertEquals("name3", permission.getName());
        Assert.assertEquals("Waiting", permission.getDescription());
        Assert.assertEquals("23-04-2022", permission.getValid_Until());
    }

    @Test
    public void testGetAllIndividualParty()
    {
        Permission permission= getPermission("code4", "name4", "Ready",
                "23-04-2001");
        permission = addPermissionAPI(permission);

        Assert.assertEquals("code4", permission.getCode());
        Assert.assertEquals("name4", permission.getName());
        Assert.assertEquals("Ready", permission.getDescription());
        Assert.assertEquals("23-04-2001", permission.getValid_Until());

        permission = getPermission("code5", "name5", "This is ready",
                "23-04-2001");
        permission = addPermissionAPI(permission);

        HttpEntity<String> entity = getStringHttpPermission();
        Permission[] result = getPermissions(entity);
        Assert.assertNotNull(result);
        assertTrue(result.length == 2);
    }

    @Test
    public void testUpdatePermission() throws RestClientException
    {
        Permission permission = getPermission("code6", "name6", "Waiting",
                "23-04-2022");
        permission = addPermissionAPI(permission);
        permission.setName("updatedPermissionName");
        permission.setDescription("updatedPermissionDescription");
        permission.setValid_Until("23-04-2023");
        String code = permission.getCode();
        HttpEntity<Permission> entity = getPermissionHttpEntity(permission);
        ResponseEntity<Permission> response = restTemplate.exchange(getRootUrl() +
                        "/permission/api/v1/managedEntityCode/entityCode1/permissionCode/" + code,
                HttpMethod.PUT, entity, Permission.class);
        Permission permission1 = restTemplate.getForObject(getRootUrl() +
                "/permission/api/v1/permissionCode/" + code, Permission.class);
        assertNotNull(permission1);
        assertEquals("code6", permission1.getCode());
        assertEquals("updatedPermissionName", permission1.getName());
        assertEquals("updatedPermissionDescription", permission1.getDescription());
        assertEquals("23-04-2023", permission1.getValid_Until());
    }

    @Test
    public void testDeletePermissionById()
    {
        Permission permission = getPermission("code7", "name7", "Waiting",
                "23-04-2022");
        permission = addPermissionAPI(permission);
        String id = permission.getId();

        delete(id);
        try
        {
            Permission permission1 = restTemplate.getForObject(getRootUrl() +
                    "/permission/api/v1/permissionId/" + id, Permission.class);
            assertFalse(permission1.isActive());
        }
        catch (final HttpClientErrorException e)
        {
            fail("object status should have been saved.");
        }
    }

    @Test
    public void testDeletePermissionByCode()
    {
        Permission permission = getPermission("code8", "name8", "Waiting",
                "23-04-2022");
        permission = addPermissionAPI(permission);
        String code = permission.getCode();

        markForDelete(code);
        try
        {
            Permission permission1 = restTemplate.getForObject(getRootUrl() +
                    "/permission/api/v1/permissionCode/" + code, Permission.class);
            assertFalse(permission1.isActive());
        }
        catch (final HttpClientErrorException e)
        {
            fail("object status should have been saved.");
        }
    }

    private void markForDelete(String code)
    {
        restTemplate.delete(getRootUrl() + "/permission/api/v1/permissionCode/" + code);
    }

    private void delete(String id)
    {
        restTemplate.delete(getRootUrl() + "/permission/api/v1/permissionId/" + id);
    }
}

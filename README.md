# SJdbc
> 基于 JdbcTemplate 的封装

问：这是什么？  
答：一个简单的基于 *JdbcTemplate* 的封装。  
  
问：这玩意有什么用呢？  
答：可以让你更方便的使用 JdbcTemplate。  
  
问：那这破玩意怎么用？  
答：（面部肌肉微微颤抖）好问题！  
首先把依赖下载一下：链接: https://pan.baidu.com/s/1cd8DhZXDQbZTl3AVE4d7Ow?pwd=1234
导入到你的项目中。  
你需要配置一下 JdbcTemplate
<h1> 配置 JdbcUtil </h1>
  
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        return new HikariDataSource();
    }
 
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
然后创建你的实体类
![image](https://user-images.githubusercontent.com/53511645/222941699-06b4eaaa-9627-4926-a775-c1bc216a0e58.png)
![image](https://user-images.githubusercontent.com/53511645/222941677-bf90fd5e-e597-4e26-baa4-29bb776aa76c.png)  
之后就可以使用了。SJdbc 有两种用法，一种是使用 QueryModel 来进行sql 拼装（只支持SELECT, UPDATE, DELETE），与 JdbcUtil 配合使用
<h1> QueryMapper and JdbcUtil </h1>
 
    private final JdbcUtil jdbcUtil;
 
    public UserService(
            JdbcUtil jdbcUtil
    ) {
        this.jdbcUtil = jdbcUtil;
    }
 
    /**
     * 只使用 jdbcUtil
     */
    public void jdbcUtil() {
        // 主键查询
        UserEntity userEntity = jdbcUtil.getById(1L, UserEntity.class);
        // 增加，可获取主键值
        UserEntity add = new UserEntity();
        add.setUsername("user");
        jdbcUtil.insert(add);
        jdbcUtil.insertBatch(Collections.singletonList(add));
        // 更新（按照主键更新）
        userEntity.setEmail("xxx@gamil.com");
        jdbcUtil.update(userEntity);
        // 删除（按照主键删除）
        jdbcUtil.delete(1L, UserEntity.class);
    }
 
    /**
     * 使用 QueryModel
     */
    public void select() {
        // sql 拼装
        QueryModel queryModel = new QueryModel(UserEntity.class);
        queryModel.select().table().from().where().eq(UserEntity::getId, 1L);
        // 查询一个，sql 等同于 SELECT * FROM t_user WHERE id = 1
        UserEntity one = jdbcUtil.getOne(queryModel, UserEntity.class);
        System.out.println(one);
 
        // 分页查询，sql 等同于 SELECT username FROM t_user WHERE username like "%user%" and createTime >= now()
        queryModel.clearSql();
        queryModel.select()
                .table("username")
                .from()
                .where()
                .like(UserEntity::getUsername, "user")
                .and()
                .ge(UserEntity::getCreateTime, new Date());
        Paging<UserEntity> paging = jdbcUtil.paging(queryModel, new Paging.Page(1, 10), UserEntity.class);
        System.out.println(paging);
 
        // 查询列表，sql 等同于 SELECT * FROM t_user WHERE id = 1 OR (username = "user" AND createTime >= now())
        queryModel.clearSql();
        queryModel.select()
                .table()
                .from()
                .where()
                .eq(UserEntity::getId, 1L).or()
                .character(SqlCharacterEnum.LEFT_BRACKETS)
                .eq(UserEntity::getUsername, "user")
                .and()
                .ge(UserEntity::getCreateTime, new Date())
                .character(SqlCharacterEnum.RIGHT_BRACKETS);
        List<UserEntity> list = jdbcUtil.getList(queryModel, UserEntity.class);
        System.out.println(list);
 
        // 更新
        queryModel.clearSql();
        queryModel.update()
                .tableName()
                .set(UserEntity::getUsername, "123")
                .where()
                .eq(UserEntity::getId, 1L);
        int update = jdbcUtil.getJdbcTemplate().update(queryModel.sql.toString());
        System.out.println(update);
 
        // 删除
        queryModel.clearSql();
        queryModel.delete().from().where().eq(UserEntity::getId, 1L);
        int delete = jdbcUtil.getJdbcTemplate().update(queryModel.sql.toString());
        System.out.println(delete);
    }
另一种使用方式类似于 JPA，在注解 JdbcMapperScan 中指定 mapper 包地址（此方式同样只支持 SELECT, UPDATE, DELETE），这种使用方式主要是考虑到需要连表查询的情况  
需要在注解 JdbcMapperScan 中指定该类所在的包地址。  
该地址下只能用来写 mapper 类  
示例：@JdbcMapperScan(basePackages = "com.xxx.mapper")  
不需要写实现类，实现类会动态生成，最终统一由 JdbcTemplate 执行 sql 语句  
在其他地方直接注入此类就可使用（无需在意 idea 红线提示的找不到 bean）  
<h1> 另一种方式 </h1>

      /**
       * 默认方式，sql 中的 ?1 指的就是第一个入参
       */
      @Sql(sql = "SELECT * FROM t_user WHERE username = ?1", type = 1)
      List<UserEntity> getListByUsername(String username);
 
      /**
       * 另一种方式，type 不为 1 的时候，可以传对象作为入参
       */
      @Sql(sql = "SELECT * FROM t_user WHERE username = param.username", type = 2)
      List<UserEntity> getListByUsernameType2(@Param(name = "param") UserEntity userEntity);
问：QueryModel 支持联表查询吗？  
答：臣妾做不到啊！（QueryModel 是考虑作为单表应用的。本身联表查询是不被推荐的，理由我能找出几十条。但如果不得不联表查询，那么使用第二中自定义 mapper 的查询方式吧）  

问：当前支持几种数据库？  
答：当前只支持 mysql。如果你想用在 oracle 上也可以，但 bug 会多到你哭。至于以后会不会支持 oracle，看我有没有精力吧。（大概率我不会写，而是放出扩展组件）  

问：这怎么会提示找不到 bean 呢？  
答：因为你写的 mapper 只定义了接口，实现类是动态生成的，idea若是没有插件支持，当然会提示这个信息。不耽误使用。  

问：可以指定多个 mapper 包吗？  
答：当然可以。  

问：你写这东西也太烂了吧？好意思发出来？  
答：我承认，我确实是个菜鸡。这个本身是我练手的项目，并没有进行深度测试，可预见的 bug 会非常多。虽然我菜，但是我永不服输。正好我脸皮厚，若是有什么意见就大方的说出来吧，谢谢了！  

-- 我是 Keguans，一名生于 99 年的菜鸡研发

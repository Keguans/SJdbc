# SJdbc
基于 JdbcTemplate 的封装

问：这是什么？ 
答：一个简单的基于 JdbcTemplate 的封装。

问：这东西能干什么？ 
答：可以让你更方便的使用 JdbcTemplate。

问：这玩意怎么用？ 
答：好问题。 
将代码下载下来，打成 jar 包直接导入即可。
SJdbc 共有两种使用方法，一种是基于 QueryModel 的拼装 sql
![image](https://user-images.githubusercontent.com/53511645/222676442-593d5fda-4e33-4ac3-a0fc-ee8f0afcb33a.png)
如图所示，使用 QueryModel 拼装 Sql，使用起来与你在 navicat 里面写 sql 是一样的思维。示例中的拼装的 sql语句等效 SELECT * FROM table WHERE id = 1 
另一种使用方式类似于 JPA
![image](https://user-images.githubusercontent.com/53511645/222673096-1ae5a006-c999-4291-a655-ba3219f1c8bf.png)
![image](https://user-images.githubusercontent.com/53511645/222674157-b83b13d6-1e5a-41f2-ba1e-07d23add7c88.png)
自定义一个mapper，并在注解 @JdbcMapperScan中 指定好 mapper类 所在的包名，将 sql 语句写在 @Sql 注解内。便可以在其他地方注入并执行。如果要使用 @Param 指定查询入参，需要设置 @Sql 中的属性 type 为2

问：QueryModel 支持联表查询吗？ 
答：臣妾做不到啊！（QueryModel 是考虑作为单表应用的。本身联表查询是不被推荐的，理由我能找出几十条。但如果不得不联表查询，那么使用第二中自定义 mapper 的查询方式吧）
![image](https://user-images.githubusercontent.com/53511645/222674114-8731c376-c364-4a2d-a956-fbeca7afc28c.png)

问：当前支持几种数据库？ 
答：当前只支持 mysql。如果你想用在 oracle 上也可以，但 bug 会多到你哭。至于以后会不会支持 oracle，看我有没有精力吧。（大概率我不会写，而是放出扩展组件）

问：这怎么会提示找不到 bean 呢？
答：因为你写的 mapper 只定义了接口，实现类是动态生成的，idea若是没有插件支持，当然会提示这个信息。不耽误使用。
![image](https://user-images.githubusercontent.com/53511645/222679910-b305131e-b280-4194-9cb7-246a272f95a7.png)

问：可以指定多个 mapper 包吗？
答：当然可以。
![image](https://user-images.githubusercontent.com/53511645/222679492-4c7577af-ecb1-4600-b179-134f49301a98.png)

问：你写这东西也太烂了吧？好意思发出来？ 
答：我承认，我确实是个菜鸡。这个本身是我练手的项目，并没有进行深度测试，可预见的 bug 会非常多。虽然我菜，但是我永不服输。正好我脸皮厚，若是有什么意见就大方的说出来吧，谢谢了！

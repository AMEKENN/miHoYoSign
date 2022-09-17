# 米游社签到(miHoYoSign)

**基于SpringBoot 和 CQHTTP 的 米游社自动签到机器人,支持原神和崩坏3,可以带小伙伴一起使用哦**

- 此软件仅供学习参考,不得用于非法盈利
- 本来只是为了自己用的,不是重大问题,大概率不会继续更新
- 写的很烂,有很多地方不符合规范,还望海涵

### 获取米游社Cookie：

**cookie抓取参考[Womsxd/AutoMihoyoBBS](https://github.com/Womsxd/AutoMihoyoBBS)**

1. 打开你的浏览器,进入**无痕/隐身模式**
2. 由于米哈游修改了bbs可以获取的Cookie，导致一次获取的Cookie缺失，所以需要增加步骤
3. 打开`http://bbs.mihoyo.com/ys/` 并进行登入操作
4. 在上一步登入完成后新建标签页，打开`http://user.mihoyo.com/` 并进行登入操作 (如果你不需要自动获取米游币可以忽略这个步骤，并把`mihoyobbs`的`enable`改为`false`即可)
5. 按下键盘上的`F12`或右键检查,打开开发者工具,点击Console
6. 输入
   ```javascript
    var cookie=document.cookie;var ask=confirm('Cookie:'+cookie+'\n\nDo you want to copy the cookie to the clipboard?');if(ask==true){copy(cookie);msg=cookie}else{msg='Cancel'}
   ```
   回车执行，并在确认无误后点击确定。
7. **此时Cookie已经复制到你的粘贴板上了**

### 获取UA：

- **用米游社APP扫描下面二维码,复制 User-Agent**
- [![image.png](https://i.postimg.cc/FKxHmRxX/image.png)](https://postimg.cc/xkc23976)

## 一. 功能

- 支持多用户,一个QQ号可以绑定一个米游社账号,让你的小伙伴可以一起享受自动签到的乐趣
- 管理员模式（管理员QQ 可以以某个QQ号的身份 向本机器人发送消息）
- 能发送通知,签到是否成功,领了个啥（默认关,且需要加为好友）
- 支持自定义延迟,支持自定义UA

## 二. 使用

### 1. 准备

- java运行环境
- MySQL （SQlite数据库会自动创建）
- OneBot Mirai (CQHTTP Mirai) 或 go-cqhttp 等 OneBot标准环境
- 注意:OneBot Mirai 的群临时消息类型不为private,而为group,不符合OneBot标准,所以必须加为好友才能使用除帮助外的命令
### 2. 编译配置文件
1.新建application.yml,放到mihoyosign.jar的同级目录

application.yml

```yml
debug: false
server:
   # 签到服务器端口
   port: 4404

##[sqlite]删除下面的注释
#spring:
#  datasource:
#    # 数据库驱动
#    driver-class-name: org.sqlite.JDBC
#    # 数据库地址
#    url: jdbc:sqlite:mihoyo_sign.sqlite
#  jpa:
#    database-platform: org.sqlite.hibernate.dialect.SQLiteDialect
#    hibernate:
#      ddl-auto: update
#    open-in-view: true

#[mysql]删除下面的注释并修改参数
spring:
   datasource:
      # 数据库驱动
      driver-class-name: com.mysql.cj.jdbc.Driver
      # 数据库地址
      url: jdbc:mysql://192.168.1.103:3306/mihoyo_sign?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8
      # 数据库用户名
      username: root
      # 数据库密码
      password: "123456789"
   jpa:
      database-platform: org.hibernate.dialect.MySQL5InnoDBDialect
      hibernate:
         ddl-auto: update
      open-in-view: true

app-config:
   # 帮助的回复
   help: |
      命令:
          绑定 [*cookie]
          解绑 (切换账号需要先解绑,更新cookie不用)
          修改UA [UA]
          修改api延迟 [延迟(单位:秒 例:0~10)]
          修改签到延迟 [延迟]
          开启签到 [游戏名] [游戏角色UID]
          关闭签到 [游戏名] [游戏角色UID]
          签到 [游戏名] [游戏角色UID]
          开启通知 [游戏名] [游戏角色UID]
          关闭通知 [游戏名] [游戏角色UID]
          所有角色
          已开启角色
      例如:
          开启签到 原神 100000000
            开启指定角色的自动签到
          开启签到 原神
            开启指定游戏的所有角色
          开启签到 100000000
            开启指定UID的所有角色
          开启签到
            开启所有角色
      注意:
          []为参数,发送的时候参数不要带[],带*的为必填参数
          抓取米游社cokkie 自行百度,方法有很多,一搜一大把
          抓取UA 用米游社扫一扫以下二维码
          [CQ:image,file=https://i.postimg.cc/FKxHmRxX/image.png]


   # OneBot Mirai地址
   url: http://192.168.1.103:4400
   # 管理员的QQ号
   admin-qq-id: 123456789
   # 日志发送到哪个群 -1:不发送到群 -2:发送给管理员
   log-group-id: 987654321
   # 默认是否发签到结果通知
   is-notice: false
   # 默认发送miHoYoApi随机延迟范围(单位:秒)
   api-delay: '[2,10]'
   # 自定义api延迟必须所在范围
   api-delay-max: '[0,120]'
   # 默认自动签到随机延迟范围(单位:秒)
   sign-delay: '[0,1200]'
   # 自定义自动签到随机延迟必须所在范围
   sign-delay-max: '[0,7200]'
   # 签到计划执行时间(每天00:00 8:00 16:00)
   sign-cron: 0 0 0,8,16 * * ?
   # 奖励列表更新频率(每月1日00:00:00,程序启动时会自动执行一次)
   sign-reward-cron: 0 0 0 1 * ?

   # 命令
   #   收到以左列关键字开头的消息,执行对应右列功能
   #   左列不能重复,右列可以重复
   #   记得结尾打英文逗号,最后一行不要打逗号
   commands: "{
    '帮助' : '帮助',
    '绑定' : '绑定',
    '解绑' : '解绑',
    '修改UA' : '修改UA',
    '修改api延迟' : '修改api延迟',
    '修改签到延迟' : '修改签到延迟',
    '开启签到' : '开启签到',
    '关闭签到' : '关闭签到',
    '签到' : '签到',
    '开启通知' : '开启通知',
    '关闭通知' : '关闭通知',
    '所有角色' : '所有角色',
    '已开启角色' : '已开启角色',
    '所有用户角色' : '所有用户角色',
    '通知' : '通知',
    '全部签到' : '全部签到'
  }"

   # 这些不需要改
   salt: 9nQiU3AV0rJSIBWgdynfoGMGKaklfbM7
   # 版本号
   version: 2.34.1
   # 默认UA 注意修改末尾版本号为{version}
   UA: Mozilla/5.0 (Linux; Android 12; MIX2 Build/SKQ1.220303.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/104.0.5112.97 Mobile Safari/537.36 miHoYoBBS/{version}
   # header
   headers: "{
    'Accept': 'application/json, text/plain, */*',
    'Content_Type': 'application/json;charset=UTF-8',
    'Connection': 'keep-alive',
    'Origin': 'https://webstatic.mihoyo.com',
    'X_Requested_With': 'com.mihoyo.hyperion',
    'Sec_Fetch_Site': 'same-site',
    'Sec_Fetch_Mode': 'cors',
    'Sec_Fetch_Dest': 'empty',
    'Accept_Encoding': 'gzip,deflate',
    'Accept_Language': 'zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7',

    'Content-Length': '66',
    'x-rpc-client_type': '5',
    'Referer': 'https://webstatic.mihoyo.com/bbs/event/signin-ys/index.html?bbs_auth_required=true&act_id=e202009291139501&utm_source=bbs&utm_medium=mys&utm_campaign=icon',
    'x-rpc-app_version': '2.34.1'
  }"
```

2.编辑OneBot Mirai 的 sitting.yml 

```yml
debug: false
proxy: ""
#机器人QQ号
'123456789':
  cacheImage: false
  cacheRecord: false
  heartbeat:
    enable: false
    interval: 15000
  http:
    #需要开启
    enable: true
    host: 0.0.0.0
    #HTTP API服务器监听端口
    port: 4400
    accessToken: ""
    postUrl: ""
    postMessageFormat: string
    secret: ""
  ws_reverse:
     # 开启反向代理
     - enable: true
       postMessageFormat: string
        # 签到服务器ip
       reverseHost: 0.0.0.0
        # 签到服务器端口
       reversePort: 4404
       accessToken: ''
        # 反向Websocket路径
       reversePath: /mihoyosign
        # 反向Websocket Api路径
       reverseApiPath: /api
        # 反向Websocket Event路径
       reverseEventPath: /event
       useUniversal: true
       useTLS: false
        # 反向 WebSocket 客户端断线重连间隔，单位毫秒
      reconnectInterval: 3000

    #其他反向代理设置
    - enable: false
      postMessageFormat: string
      ...
      ...
```
具体参数参考[OneBot Mirai](https://github.com/yyuueexxiinngg/onebot-kotlin)

### 3. 运行jar包
命令:
```
java -jar miHoYoSign.jar
```
### 4.运行mirai

## 三. 命令格式
```
用户命令:
   参考 application.yml 配置文件中的 help 参数

管理员命令:
    所有用户角色
    通知 [通知内容] [QQ号]
    全部签到
    (QQ:[QQ号])命令  #这是以某个QQ号的身份发送命令

  ```

## 四. 预览图

[![2022-09-14-230435.png](https://i.postimg.cc/KzVxfgNF/2022-09-14-230435.png)](https://postimg.cc/34gsJNVb)

## 五. 注意事项

- 注意服务器时间要为中国时间

## 直接或间接引用到的其他开源项目

- [OneBot Mirai](https://github.com/yyuueexxiinngg/onebot-kotlin)
- [genshin-impact-helper](https://github.com/y1ndan/genshin-impact-helper)
- [Womsxd/AutoMihoyoBBS](https://github.com/Womsxd/AutoMihoyoBBS)

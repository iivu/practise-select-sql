
package com.github.hcsp.sql;


import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Sql {
// 用户表：
// +----+----------+------+----------+
// | ID | NAME     | TEL  | ADDRESS  |
// +----+----------+------+----------+
// | 1  | zhangsan | tel1 | beijing  |
// +----+----------+------+----------+
// | 2  | lisi     | tel2 | shanghai |
// +----+----------+------+----------+
// | 3  | wangwu   | tel3 | shanghai |
// +----+----------+------+----------+
// | 4  | zhangsan | tel4 | shenzhen |
// +----+----------+------+----------+
// 商品表：
// +----+--------+-------+
// | ID | NAME   | PRICE |
// +----+--------+-------+
// | 1  | goods1 | 10    |
// +----+--------+-------+
// | 2  | goods2 | 20    |
// +----+--------+-------+
// | 3  | goods3 | 30    |
// +----+--------+-------+
// | 4  | goods4 | 40    |
// +----+--------+-------+
// | 5  | goods5 | 50    |
// +----+--------+-------+
// 订单表：
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | ID(订单ID) | USER_ID(用户ID) | GOODS_ID(商品ID) | GOODS_NUM(商品数量) | GOODS_PRICE(下单时的商品单价)        |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 1          | 1               | 1                | 5                   | 10                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 2          | 2               | 1                | 1                   | 10                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 3          | 2               | 1                | 2                   | 10                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 4          | 4               | 2                | 4                   | 20                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 5          | 4               | 2                | 100                 | 20                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 6          | 4               | 3                | 1                   | 20                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 7          | 5               | 4                | 1                   | 20                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+
// | 8          | 5               | 6                | 1                   | 60                            |
// +------------+-----------------+------------------+---------------------+-------------------------------+

    // 用户信息
    public static class User {
        Integer id;
        String name;
        String tel;
        String address;

        @Override
        public String toString() {
            return "User{" + "id=" + id + ", name='" + name + '\'' + ", tel='" + tel + '\'' + ", address='" + address + '\'' + '}';
        }
    }

    /**
     * 题目1：
     * 查询有多少所有用户曾经买过指定的商品
     *
     * @param goodsId            指定的商品ID
     * @param databaseConnection 数据库连接
     * @return 有多少用户买过这个商品
     */
// 例如，输入goodsId = 1，返回2，因为有2个用户曾经买过商品1。
// +-----+
// |count|
// +-----+
// | 2   |
// +-----+
    public static int countUsersWhoHaveBoughtGoods(Connection databaseConnection, Integer goodsId) throws SQLException {
        PreparedStatement statement = databaseConnection.prepareStatement("SELECT COUNT(DISTINCT `user_id`) as `count` FROM `order` WHERE `goods_id` = ?;");
        statement.setInt(1, goodsId);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt("count");
        }
        return 0;
    }

    /**
     * 题目2：
     * 分页查询所有用户，按照ID倒序排列
     *
     * @param pageNum            第几页，从1开始
     * @param pageSize           每页有多少个元素
     * @param databaseConnection 数据库连接
     * @return 指定页中的用户
     */
// 例如，pageNum = 2, pageSize = 3（每页3个元素，取第二页），则应该返回：
// +----+----------+------+----------+
// | ID | NAME     | TEL  | ADDRESS  |
// +----+----------+------+----------+
// | 1  | zhangsan | tel1 | beijing  |
// +----+----------+------+----------+
    public static List<User> getUsersByPageOrderedByIdDesc(Connection databaseConnection, int pageNum, int pageSize) throws SQLException {
        PreparedStatement statement = databaseConnection.prepareStatement("SELECT *\n" +
                "FROM `user`\n" +
                "ORDER BY `id` DESC\n" +
                "LIMIT ? OFFSET ?");
        statement.setInt(1, pageSize);
        statement.setInt(2, (pageNum - 1) * pageSize);
        ResultSet resultSet = statement.executeQuery();
        List<User> result = new ArrayList<>();
        while (resultSet.next()) {
            User u = new User();
            u.id = resultSet.getInt("id");
            u.name = resultSet.getString("name");
            u.address = resultSet.getString("address");
            u.tel = resultSet.getString("tel");
            result.add(u);
        }
        return result;
    }

    // 商品及其营收
    public static class GoodsAndGmv {
        Integer goodsId; // 商品ID
        String goodsName; // 商品名
        BigDecimal gmv; // 商品的所有销售额

        @Override
        public String toString() {
            return "GoodsAndGmv{" + "goodsId=" + goodsId + ", goodsName='" + goodsName + '\'' + ", gmv=" + gmv + '}';
        }
    }

    /**
     * 题目3：
     * 查询所有的商品及其销售额，按照销售额从大到小排序
     *
     * @param databaseConnection 数据库连接
     * @return 指定的数据列表
     */
// 预期的结果应该如图所示
//  +----+--------+------+
//  | ID | NAME   | GMV  |
//  +----+--------+------+
//  | 2  | goods2 | 2080 |
//  +----+--------+------+
//  | 1  | goods1 | 80   |
//  +----+--------+------+
//  | 4  | goods4 | 20   |
//  +----+--------+------+
//  | 3  | goods3 | 20   |
//  +----+--------+------+
    public static List<GoodsAndGmv> getGoodsAndGmv(Connection databaseConnection) throws SQLException {
        PreparedStatement statement = databaseConnection.prepareStatement("SELECT `goods`.id, `goods`.name, SUM((`order`.`goods_num` * `order`.`goods_price`)) as `GMV` FROM `order` JOIN `goods` ON `order`.`goods_id` = `goods`.`id` GROUP BY `goods`.`id` ORDER BY `GMV` DESC");
        ResultSet resultSet = statement.executeQuery();
        List<GoodsAndGmv> result = new ArrayList<>();
        while (resultSet.next()) {
            GoodsAndGmv o = new GoodsAndGmv();
            o.gmv = resultSet.getBigDecimal("gmv");
            o.goodsId = resultSet.getInt("id");
            o.goodsName = resultSet.getString("name");
            result.add(o);
        }
        return result;
    }


    // 订单详细信息
    public static class Order {
        Integer id; // 订单ID
        String userName; // 用户名
        String goodsName; // 商品名
        BigDecimal totalPrice; // 订单总金额

        @Override
        public String toString() {
            return "Order{" + "id=" + id + ", userName='" + userName + '\'' + ", goodsName='" + goodsName + '\'' + ", totalPrice=" + totalPrice + '}';
        }
    }

    /**
     * 题目4：
     * 查询订单信息，只查询用户名、商品名齐全的订单，即INNER JOIN方式
     *
     * @param databaseConnection 数据库连接
     * @return 指定的数据列表
     */
// 预期的结果为：
// +----------+-----------+------------+-------------+
// | ORDER_ID | USER_NAME | GOODS_NAME | TOTAL_PRICE |
// +----------+-----------+------------+-------------+
// | 1        | zhangsan  | goods1     | 50          |
// +----------+-----------+------------+-------------+
// | 2        | lisi      | goods1     | 10          |
// +----------+-----------+------------+-------------+
// | 3        | lisi      | goods1     | 20          |
// +----------+-----------+------------+-------------+
// | 4        | zhangsan  | goods2     | 80          |
// +----------+-----------+------------+-------------+
// | 5        | zhangsan  | goods2     | 2000        |
// +----------+-----------+------------+-------------+
// | 6        | zhangsan  | goods3     | 20          |
// +----------+-----------+------------+-------------+
    public static List<Order> getInnerJoinOrders(Connection databaseConnection) throws SQLException {
        PreparedStatement statement = databaseConnection.prepareStatement("SELECT `order`.`id` as `order_id`, `user`.`name` as `user_name`, `goods`.`name` as `goods_name`,(`order`.`goods_num` * `order`.`goods_price`) as `total_price`\n" +
                "FROM `order`\n" +
                "JOIN `user` ON `order`.`user_id` = `user`.`id`\n" +
                "JOIN `goods` ON `order`.`goods_id` = `goods`.`id`;");
        ResultSet resultSet = statement.executeQuery();
        return getOrderList(resultSet);
    }

    /**
     * 题目5：
     * 查询所有订单信息，哪怕它的用户名、商品名缺失，即LEFT JOIN方式
     *
     * @param databaseConnection 数据库连接
     * @return 指定的数据列表
     */
// 预期的结果为：
// +----------+-----------+------------+-------------+
// | ORDER_ID | USER_NAME | GOODS_NAME | TOTAL_PRICE |
// +----------+-----------+------------+-------------+
// | 1        | zhangsan  | goods1     | 50          |
// +----------+-----------+------------+-------------+
// | 2        | lisi      | goods1     | 10          |
// +----------+-----------+------------+-------------+
// | 3        | lisi      | goods1     | 20          |
// +----------+-----------+------------+-------------+
// | 4        | zhangsan  | goods2     | 80          |
// +----------+-----------+------------+-------------+
// | 5        | zhangsan  | goods2     | 2000        |
// +----------+-----------+------------+-------------+
// | 6        | zhangsan  | goods3     | 20          |
// +----------+-----------+------------+-------------+
// | 7        | NULL      | goods4     | 20          |
// +----------+-----------+------------+-------------+
// | 8        | NULL      | NULL       | 60          |
// +----------+-----------+------------+-------------+
    public static List<Order> getLeftJoinOrders(Connection databaseConnection) throws SQLException {
        PreparedStatement statement = databaseConnection.prepareStatement("SELECT `order`.`id` as `order_id`, `user`.`name` as `user_name`, `goods`.`name` as `goods_name`,(`order`.`goods_num` * `order`.`goods_price`) as `total_price`\n" +
                "FROM `order`\n" +
                "LEFT JOIN `user` ON `order`.`user_id` = `user`.`id`\n" +
                "LEFT JOIN `goods` ON `order`.`goods_id` = `goods`.`id`;");
        ResultSet resultSet = statement.executeQuery();
        return getOrderList(resultSet);
    }

    public static List<Order> getOrderList(ResultSet resultSet) throws SQLException {
        List<Order> result = new ArrayList<>();
        while (resultSet.next()) {
            Order o = new Order();
            o.id = resultSet.getInt("order_id");
            o.goodsName = resultSet.getString("goods_name");
            o.userName = resultSet.getString("user_name");
            o.totalPrice = resultSet.getBigDecimal("total_price");
            result.add(o);
        }
        return result;
    }

    // 注意，运行这个方法之前，请先运行mvn initialize把测试数据灌入数据库
    public static void main(String[] args) throws SQLException {
        File projectDir = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        String jdbcUrl = "jdbc:h2:file:" + new File(projectDir, "target/test").getAbsolutePath();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "root", "Jxi1Oxc92qSj")) {
            System.out.println(countUsersWhoHaveBoughtGoods(connection, 1));
            System.out.println(getUsersByPageOrderedByIdDesc(connection, 2, 3));
            System.out.println(getGoodsAndGmv(connection));
            System.out.println(getInnerJoinOrders(connection));
            System.out.println(getLeftJoinOrders(connection));
        }
    }

}

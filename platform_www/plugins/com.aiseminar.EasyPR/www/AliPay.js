cordova.define("com.aiseminar.EasyPR.alipay", function(require, exports, module) {
var exec = require('cordova/exec');

/**

navigator.weixin.pay({"seller":"007slm@163.com",subject":"x51","body":"x5��ҵ��","price":"0.01","tradeNo":"123456","timeout":"30m","notifyUrl":"wwww.justep.com"},function(msgCode){alert(msgCode)},function(msg){alert(msg)})
**/

module.exports = {
    pay: function(orderInfo,onSuccess,onError){
        exec(onSuccess, onError, "Recognition", "pay", [orderInfo]);
    }
};

});

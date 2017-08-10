
//导航切换-----------------------------------------------
showHide=function (objct,ac){
    objct.on("click",function () {
        var index=$(this).index();
        objct.removeClass(ac);
        $(this).addClass(ac);
    });
};
//扩展方法获取url参数--------------------------------------
$.getUrlParam = function (name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r != null) return unescape(r[2]); return null;
};


function getPhoto(node,objct) {
    var imgURL = "";
    try {
        var file = null;
        if(node.files && node.files[0]) {
            file = node.files[0];
        } else if(node.files && node.files.item(0)) {
            file = node.files.item(0);
        }
        //Firefox 因安全性问题已无法直接通过input[file].value 获取完整的文件路径
        try {
            imgURL = file.getAsDataURL();
        } catch(e) {
            imgRUL = window.URL.createObjectURL(file);
        }
    } catch(e) {
        if(node.files && node.files[0]) {
            var reader = new FileReader();
            reader.onload = function(e) {
                imgURL = e.target.result;
            };
            reader.readAsDataURL(node.files[0]);
        }
    }
    objct.attr('src', imgRUL);
    return imgURL;
}



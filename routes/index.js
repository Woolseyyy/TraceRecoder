var express = require('express');
var User = require('../db/user');
var Info = require('../db/info');
var CitizenInfo = require('../db/citizenInfo');
var router = express.Router();
var textEncoding = require('text-encoding');
var TextDecoder = textEncoding.TextDecoder;

/* GET home page. */
/*router.get('/', function(req, res, next) {
    User.find({areaID:null},function(err, users){
        if(err){
            res.render('error', { message: '数据库查询错误', error:err });
        }
        else {
            console.log(users);
            res.render('index', { users:users });
        }
    });
});

router.get('/', function(req, res, next) {
    User.find({areaID:{"$ne": null}},function(err, users){
        if(err){
            res.render('error', { message: '数据库查询错误', error:err });
        }
        else {
            console.log(users);
            res.render('index', { users:users });
        }
    });
});*/

router.get('/', function(req, res, next) {
    User.find({},function(err, users){
        if(err){
            res.render('error', { message: '数据库查询错误', error:err });
        }
        else {
            console.log(users);
            res.render('index', { users:users });
        }
    });
});

router.get('/user', function(req, res, next) {
  var id = req.query.id;
  User.findOne({
    id:id
  },function(err, user){
    if(err){
      res.json({
        code: -1,
        msg: '查询错误:' + err,
        body: {}
      });
    }
    else{
      if(user){
        res.json({
          code: 0,
          msg: 'ok',
          body: {
            areaID:user.areaID,
            id:user.id,
            maxTripNum: user.maxTripNum
          }
        });
      }
      else{
        var newUser = {
          areaID:null,
          id: id,
          maxTripNum : 0,
          trip:[]
        };
        User.create(newUser, function(err){
          if(err){
            res.json({
              code: -1,
              msg: '新增错误:' + err,
              body: {}
            });
          }
          else{
            res.json({
              code: 0,
              msg: 'ok',
              body: newUser
            });
          }
        })
      }
    }
  });



});

router.post('/save', function(req, res, next) {
  console.log(req.body);
  var InTrip = req.body.trip;
  var InUser = req.body.user;
  console.log(InUser.id);
  User.findOne({
    id:InUser.id
  }, function(err, user){
    if(err){
      res.json({
        code: -1,
        msg: '查询错误:' + err,
        body: {}
      });
    }
    else{
      console.log(user);
      if(user){
        user.maxTripNum = InUser.maxTripNum;
        user.trip.push(InTrip);
        user.save(function(err){
          if(err){
              res.json({
              code: -1,
              msg: '修改错误:' + err,
              body: {}
            });
          }
          else{
              res.json({
              code: 0,
              msg: 'ok',
              body: {}
            });
          }
        });
      }
      else{
        var newUser = {
          id: InUser.id,
          maxTripNum : InUser.maxTripNum,
          trip:[InTrip]
        };
        User.create(newUser, function(err){
          if(err){
            res.json({
              code: -1,
              msg: '新增错误:' + err,
              body: {}
            });
          }
        })
      }

    }
  })
});

router.post('/recognition', function(req, res, next) {
    var tripID = (req.body.tripID)?req.body.tripID.toString():"None";
    var data = req.body.data;

    var dataForTest = {
        data:[
            {
                segment_ID: tripID,
                trans_mode: 'None',
                former_trans_mode: 'None',
                data: data
            }
        ]
    };

    //var dataForTest = {"data":[{"segment_ID":"Data085_20081211234130_bus","trans_mode":"bus","former_trans_mode":"None","data":[{"latitude":39.897337,"longitude":116.343463,"date":"2008-12-11","time":"23:41:30"},{"latitude":39.898337,"longitude":116.343463,"date":"2008-12-11","time":"23:41:32"},{"latitude":39.899337,"longitude":116.343463,"date":"2008-12-11","time":"23:41:34"}]}]};

    //var args = JSON.stringify(dataForTest).replace(/\"/g, "\\\"");
    var args = JSON.stringify(dataForTest);

    /* const spawn = require('child_process').spawn;
    var child = spawn('python', ['../recognition/one_segment.py', args]);
    child.stdout.on('data', function(stdout){
        var mode = stdout;
        res.json({
            code: 0,
            msg:'ok',
            body:{
                mode: mode
            }
        })
    });
    child.stderr.on('data', function(stderr){
        console.error('Error when recognition!\nMessage: ' + stderr + '\nData: ' + dataForTest);
        res.json({
            code: -1,
            msg:'error',
            body:{
                error: stderr
            }
        })
    })
    */
    var spawnSync = require('spawn-sync');
    var result = spawnSync('python3', ['recognition/one_segment.py', args]);
    if (result.status !== 0) {
        var error = new TextDecoder("utf-8").decode(result.stderr);
        console.error('Error when recognition!\nMessage: ' + error + '\nData: ' + dataForTest);
        res.json({
            code: -1,
            msg:'error',
            body:{
                error: error
            }
        })
    } else {
        var mode = new TextDecoder("utf-8").decode(result.stdout);
        if(mode[mode.length-1] === '\n'){
            mode = mode.slice(0, mode.length-1);
        }
        if(mode[mode.length-1] === '\r'){
            mode = mode.slice(0, mode.length-1);
        }
        
        var modeDic = {
            'bike': 2, 
            'bus': 7, 
            'car': 6, 
            'walk': 1};

        res.json({
            code: 0,
            msg:'ok',
            body:{
                mode: mode
                modeCode: modeDic[mode]
            }
        })
    }
});

router.get('/info', function(req, res, next) {
  var id = req.query.id;
  res.render('info', { id:id });
});

router.get('/citizen/info', function(req, res, next) {
    var id = req.query.id;
    res.render('citizenInfo', { id:id });
});

router.post('/info', function(req, res, next) {
  var form = req.body.form;
  console.log(form);
  Info.create(form, function(err){
    if(err){
      res.json({
        code: -1,
        msg: '新增错误:' + err,
        body: {}
      });
    }
    else{
      res.json({
        code: 0,
        msg: 'ok',
        body: {}
      });
    }
  })
});

router.post('/citizen/info', function(req, res, next) {
    var form = req.body.form;
    console.log(form);
    CitizenInfo.create(form, function(err){
        if(err){
            res.json({
                code: -1,
                msg: '新增错误:' + err,
                body: {}
            });
        }
        else{
            res.json({
                code: 0,
                msg: 'ok',
                body: {}
            });
        }
    })
});

router.get('/info/success', function(req, res, next) {
  res.render('success');
});

router.get('/info/error', function(req, res, next) {
  res.render('error',{message:"添加失败", error:{status:"", stack:""}});
});

router.get('/info/watch', function(req, res, next) {
  Info.find(null,function(err, info){
    if(err){
      res.render('error', { message: '数据库查询错误', error:err });
    }
    else {
      console.log(info);
      res.render('infoWatch', { infos:info });
    }
  });
});

router.get('/citizen/info/watch', function(req, res, next) {
    CitizenInfo.find(null,function(err, info){
        if(err){
            res.render('error', { message: '数据库查询错误', error:err });
        }
        else {
            console.log(info);
            res.render('citizenInfoWatch', { infos:info });
        }
    });
});

router.post('/info/delete', function(req, res, next) {
  var _id = req.body._id;
  console.log(_id);
  Info.remove({_id:_id}, function(err){
    if(err){
      res.json({
        code: -1,
        msg: '删除错误:' + err,
        body: {}
      });
    }
    else{
      res.json({
        code: 0,
        msg: 'ok',
        body: {}
      });
    }
  })
});

router.post('/citizen/info/delete', function(req, res, next) {
    var _id = req.body._id;
    console.log(_id);
    CitizenInfo.remove({_id:_id}, function(err){
        if(err){
            res.json({
                code: -1,
                msg: '删除错误:' + err,
                body: {}
            });
        }
        else{
            res.json({
                code: 0,
                msg: 'ok',
                body: {}
            });
        }
    })
});




module.exports = router;

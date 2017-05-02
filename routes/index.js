var express = require('express');
var User = require('../db/user');
var Info = require('../db/info');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  User.find(null,function(err, users){
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
          areaID:'',
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
          res.json({
            code: -1,
            msg: '修改错误:' + err,
            body: {}
          });
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

router.get('/info', function(req, res, next) {
  var id = req.query.id;
  res.render('info', { id:id });
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




module.exports = router;

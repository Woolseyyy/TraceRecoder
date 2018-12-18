/**
 * Created by admin on 2017/5/1.
 */
/**
 * Created by admin on 2017/5/1.
 */
var mongoose = require('mongoose');
mongoose.Promise = global.Promise;
var Schema = mongoose.Schema;
module.exports = mongoose.model('info', new Schema({
    id: String,
    sex: Number,
    year:Number,
    area: Number,
    grade: Number,
    major: Number,
    mainWay: Number,
    majorName:  String,
    has: Number,
    carNum: Number,
    mobikeNum: Number,
    bikeNum: Number
}));
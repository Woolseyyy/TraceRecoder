/**
 * Created by admin on 2017/5/1.
 */
/**
 * Created by admin on 2017/5/1.
 */
var mongoose = require('mongoose');
mongoose.Promise = global.Promise;
var Schema = mongoose.Schema;
module.exports = mongoose.model('citizenInfo', new Schema({
    id: String,
    sex: Number,
    year:Number,
    address:{
        block:String,
        road:String,
        number:String,
        name:String
    },
    education: Number,
    job: Number,
    familyIncome: Number,
    personIncome: Number,
    familyNum: Number,
    familyAttr: Number,
    has: Number,
    carNum: Number,
    mobikeNum: Number,
    bikeNum: Number,
    orgAddress:{
        block:String,
        road:String,
        number:String,
        name:String
    },
    areaID:String,
    familyID:String
}));
/**
 * Created by admin on 2017/5/1.
 */
var mongoose = require('mongoose');
mongoose.Promise = global.Promise;
var Schema = mongoose.Schema;
module.exports = mongoose.model('user', new Schema({
    areaID: {type:String, default:null},
    id: {type:String, unique:true},
    maxTripNum : Number,
    trip:[{
        id: Number,
        date: String,
        purpose: Number,
        sLocation:{
            data:[],
            describe:String
        },
        eLocation:{
            data:[],
            describe:String
        },
        sDate: String,
        eDate: String,
        waysCount: Number,
        deltaTime:[],
        distance: Number,
        children:[{
            id: Number,
            sLocation:{
                data:[],
                describe:String
            },
            eLocation:{
                data:[],
                describe:String
            },
            sDate: String,
            eDate: String,
            way: Number,
            place:Number,
            deltaTime:[],
            distance: Number
        }]
    }]
}));
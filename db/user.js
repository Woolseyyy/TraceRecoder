/**
 * Created by admin on 2017/5/1.
 */
var mongoose = require('mongoose');
mongoose.Promise = global.Promise;
var Schema = mongoose.Schema;
module.exports = mongoose.model('user', new Schema({
    areaID: String,
    id: {type:String, unique:true},
    maxTripNum : Number,
    trip:[{
        id: {type:Number, unique:true},
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
            deltaTime:[],
            distance: Number
        }]
    }]
}));
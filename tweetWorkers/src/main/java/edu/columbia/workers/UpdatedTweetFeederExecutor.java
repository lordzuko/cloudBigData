package edu.columbia.workers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.Gson;
import edu.columbia.cbd.models.Constants;
import edu.columbia.cbd.models.Tweet;
import edu.columbia.cbd.service.MongoService;
import edu.columbia.cbd.service.SQSService;
import edu.columbia.cbd.service.impl.MongoServiceImpl;
import edu.columbia.cbd.service.impl.SQSServiceImpl;

/*
Author: Diwakar Mahajan (@diwakar21)
 */

public class UpdatedTweetFeederExecutor implements Runnable{
	private Tweet tweet;
	private MongoService mongoService;
    private static int WAIT_TIME = 1000;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//System.out.println(Thread.currentThread().getName()+" Start. Thread");
		updateMongo();
        //System.out.println(Thread.currentThread().getName()+" End.");
    
	}
	public UpdatedTweetFeederExecutor(Tweet tweet, MongoService mongoService){
		this.tweet=tweet;
        this.mongoService = mongoService;
	}

	private void updateMongo() {
        mongoService.updateTweet(tweet);
	}
	
    public static void main(String[] args) throws InterruptedException, JSONException {
        System.out.println("Starting Tweet Updation now!");
        WorkerBootStrap workerBootStrap = WorkerBootStrap.getInstance();
        workerBootStrap.startUp();
        SQSService sqsServiceIncoming = new SQSServiceImpl();
        MongoService mongoService = new MongoServiceImpl();
        Gson gson = new Gson();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        while(true) {
            boolean worked = false;
            List<Message> msgList = sqsServiceIncoming.receiveMessage(Constants.TWITTER_OUTGOING_QUEUE_URL);
            for (Message msg : msgList) {
                worked = true;
                WAIT_TIME = 1000;

                JSONObject json = new JSONObject(msg.getBody());

                String text = json.getString("Message");
                
                //Convert to thread pool 
               
                Tweet tweet = gson.fromJson(text, Tweet.class);
                Runnable UpdatedTweetFeederExecutor = new UpdatedTweetFeederExecutor(tweet , mongoService);
                executor.execute(UpdatedTweetFeederExecutor);
                sqsServiceIncoming.deleteMessage(Constants.TWITTER_OUTGOING_QUEUE_URL, msg.getReceiptHandle());
            }
            if(!worked){
                System.out.println("Waiting for "+WAIT_TIME/1000 +" seconds");
                Thread.sleep(WAIT_TIME);
                WAIT_TIME = WAIT_TIME * 2;
                if(WAIT_TIME > 64000){
                    WAIT_TIME =  64000;
                }

            }
        }

    }
}

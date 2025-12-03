public class Post {

    // "tiktok", "x", later maybe "facebook", ...
    public String platform;

    // Text content of the post / tweet / video description
    public String content;

    // Date/time as formatted string (whatever you already use, e.g. ISO 8601)
    public String createdDate;

    // Reactions count (likes / diggs / retweets / etc.)
    public long reaction;

    public Post() {
        // no-arg constructor in case some code does:
        // Post p = new Post(); p.content = ...
    }

    public Post(String platform, String content, String createdDate, long reaction) {
        this.platform = platform;
        this.content = content;
        this.createdDate = createdDate;
        this.reaction = reaction;
    }
}

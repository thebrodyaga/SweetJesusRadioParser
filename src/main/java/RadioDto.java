public class RadioDto {

    private String radioName;
    private String radioUrl;
    private String radioImage;
    private String smallRadioImage;

    public RadioDto(String radioName, String radioUrl, String radioImage, String smallRadioImage) {
        this.radioName = radioName;
        this.radioUrl = radioUrl;
        this.radioImage = radioImage;
        this.smallRadioImage = smallRadioImage;
    }
}

 import org.primefaces.util.HTML;
         
     public String resolveIcon() {
         String icon = getIcon();
     
         if(icon == null) {
             icon = getImage();
         }
     
         return icon;
     }
 
     public String resolveStyleClass() {
         String icon = resolveIcon();
         Object value = getValue();
         String styleClass = ""; 
     
         if(value != null && icon == null) {
             styleClass = HTML.BUTTON_TEXT_ONLY_BUTTON_CLASS;
         }
         else if(value != null && icon != null) {
             styleClass = getIconPos().equals("left") ? HTML.BUTTON_TEXT_ICON_LEFT_BUTTON_CLASS : HTML.BUTTON_TEXT_ICON_RIGHT_BUTTON_CLASS;
         }
         else if(value == null && icon != null) {
             styleClass = HTML.BUTTON_ICON_ONLY_BUTTON_CLASS;
         }
     
         if(isDisabled()) {
             styleClass = styleClass + " ui-state-disabled";
         } 
     
         String userStyleClass = getStyleClass();
         if(userStyleClass != null) {
             styleClass = styleClass + " " + userStyleClass;
         }
     
         return styleClass;
     }

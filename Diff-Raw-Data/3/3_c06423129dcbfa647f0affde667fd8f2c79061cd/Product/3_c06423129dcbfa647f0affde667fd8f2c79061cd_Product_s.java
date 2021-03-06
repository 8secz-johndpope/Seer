 /**
  * Copyright (c) 2009 Red Hat, Inc.
  *
  * This software is licensed to you under the GNU General Public License,
  * version 2 (GPLv2). There is NO WARRANTY for this software, express or
  * implied, including the implied warranties of MERCHANTABILITY or FITNESS
  * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
  * along with this software; if not, see
  * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
  *
  * Red Hat trademarks are not licensed under GPLv2. No permission is
  * granted to use or replicate Red Hat trademarks that are incorporated
  * in this software or its documentation.
  */
 package org.fedoraproject.candlepin.model;
 
 import java.util.HashSet;
 import java.util.Set;
 
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.FetchType;
 import javax.persistence.Id;
 import javax.persistence.JoinColumn;
 import javax.persistence.JoinTable;
 import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
 import javax.persistence.OneToMany;
 import javax.persistence.SequenceGenerator;
 import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
 import javax.xml.bind.annotation.XmlAccessType;
 import javax.xml.bind.annotation.XmlAccessorType;
 import javax.xml.bind.annotation.XmlRootElement;
 import javax.xml.bind.annotation.XmlTransient;
 
 import org.hibernate.annotations.ForeignKey;
 
 /**
  * Represents a Product that can be consumed and entitled. Products define
  * the software or entity they want to entitle i.e. RHEL Server. They also 
  * contain descriptive meta data that might limit the Product i.e. 4 cores
  * per server with 4 guests. 
  */
 @XmlRootElement
 @XmlAccessorType(XmlAccessType.PROPERTY)
 @Entity
 @Table(name = "cp_product")
 @SequenceGenerator(name = "seq_product", sequenceName = "seq_product", allocationSize = 1)
 public class Product extends AbstractHibernateObject {
    
     // Product ID is stored as a string. Could be a product OID or label.
     @Id
     private String id;
     
     @Column(nullable = false, unique = true)
     private String label;
     
     @Column(nullable = false, unique = true)
     private String name;
     
     // Server, Client, Cloud, whatever...
     @Column(nullable = true)
     private String variant;
     
     @Column(nullable = true)
     private String version;
     
     // suppose we could have an arch table
     @Column(nullable = true)
     private String arch;
     
     // whatever numeric identifier we come up with for
     // use in the cert's OID structure...
     @Column(nullable = true, unique = true)
     private Long hash;
 
     //FIXME: nullable just to bootstrap for now
     @Column(nullable = true)
     private String type;
  
     public String getType() {
         return type;
     }
 
     public void setType(String type) {
         this.type = type;
     }
 
     // NOTE: we need a product "type" so we can tell what class of
     //       product we are... 
     @ManyToMany(targetEntity = Product.class, cascade = CascadeType.ALL,
             fetch = FetchType.EAGER)
     @ForeignKey(name = "fk_product_product_id",
                 inverseName = "fk_product_child_product_id")
     @JoinTable(name = "cp_product_hierarchy",
             joinColumns = @JoinColumn(name = "parent_product_id"),
             inverseJoinColumns = @JoinColumn(name = "child_product_id"))
     private Set<Product> childProducts;
 
     @OneToMany(cascade = CascadeType.ALL)
     @JoinTable(name = "cp_product_attribute")
     private Set<Attribute> attributes;
 
 //    @OneToMany(cascade = CascadeType.ALL)
     @ManyToMany(cascade = CascadeType.ALL)
     @JoinTable(name = "cp_product_content")
     private Set<Content> content;
  
   
     @ManyToMany(cascade = CascadeType.ALL)
     @JoinTable(name = "cp_product_enabled_content")
     private Set<Content> enabledContent;
     
    
     /**
      * Constructor
      * 
      * Use this variant when creating a new object to persist.
      * 
      * @param label Product label
      * @param name Human readable Product name
      */
     public Product(String label, String name) {
         setId(label);
         setLabel(label);
         setName(name);
     }
     
     public Product(String label, String name, String variant,
                    String version, String arch, Long hash,
                    String type, Set<Product> childProducts,
                    Set<Content> content) {
         setId(label);
         setLabel(label);
         setName(name);
         setVariant(variant);
         setVersion(version);
         setArch(arch);
         setHash(hash);
         setType(type);
         setChildProducts(childProducts);
         setContent(content);
         // FIXME
         setEnabledContent(content);
     }
 
     protected Product() {
     }
 
     /** {@inheritDoc} */
     public String getId() {
         return id;
     }
 
     /**
      * @param id product id
      */
     public void setId(String id) {
         this.id = id;
     }
 
     /**
      * @return set of child products.
      */
     public Set<Product> getChildProducts() {
         return childProducts;
     }
 
     
     public Set<Product> getAllChildProducts(Set<Product> products) {
         products.add(this);
         if ((childProducts == null) || (childProducts.isEmpty())) {
             return products;
         }
         
         for (Product childProduct : childProducts) {
             Set<Product> ps = new HashSet<Product>();
             ps = childProduct.getAllChildProducts(products);
             for (Product p : ps) { 
                 products.add(p);
             }
         }
         return products;
     }   
     
 
     /**
      * replaces all of the product children with the new set.
      * @param childProducts new child products.
      */
     public void setChildProducts(Set<Product> childProducts) {
         this.childProducts = childProducts;
     }
 
     /**
      * Add the given product as a child of this product.
      * @param p child product to add.
      */
     public void addChildProduct(Product p) {
         if (this.childProducts ==  null) {
             this.childProducts = new HashSet<Product>();
         }
         this.childProducts.add(p);
     }
     
     /**
      * @return the product label
      */
     public String getLabel() {
         return label;
     }
 
     /**
      * @param labelIn product label
      */
     public void setLabel(String labelIn) {
         label = labelIn;
     }
 
     @Override
     public String toString() {
         return "Product [label = " + label + "]";
     }
     
     /**
      * @return the product name
      */
     public String getName() {
         return name;
     }
 
     /**
      * sets the product name.
      * @param name name of the product
      */
     public void setName(String name) {
         this.name = name;
     }
 
     /**
      * @return the Product attributes
      */
     public Set<Attribute> getAttributes() {
         return attributes;
     }
     
     @XmlTransient
     public Set<String> getAttributeNames() {
         Set<String> toReturn = new HashSet<String>();
         if (attributes == null) {
             return toReturn;
         }
         
         for (Attribute attribute : attributes) {
             toReturn.add(attribute.getName());
         }
         return toReturn;
     }
 
     /**
      * Replaces all of the product attributes with the given set.
      * @param attributes attributes which will replace the current set.
      */
     public void setAttributes(Set<Attribute> attributes) {
         this.attributes = attributes;
     }
 
     /**
      * add an attribute to the product
      * @param attrib attribute to be added.
      */
     public void addAttribute(Attribute attrib) {
         if (this.attributes == null) {
             this.attributes = new HashSet<Attribute>();
         }
         this.attributes.add(attrib);
     }
     
     public Attribute getAttribute(String key) {
         for (Attribute a : attributes) {
             if (a.getName().equals(key)) {
                 return a;
             }
         }
         return null;
     }
 
     @Override
     public boolean equals(Object anObject) {
         if (this == anObject) {
             return true;
         }
         if (!(anObject instanceof Product)) {
             return false;
         }
 
         Product another = (Product) anObject;
 
         return
             label.equals(another.getLabel()) &&
             name.equals(another.getName());
     }
 
     @Override
     public int hashCode() {
         return label.hashCode() * 31 + name.hashCode();
     }
 
     /**
      * @param hash the hash to set
      */
     public void setHash(Long hash) {
         this.hash = hash;
     }
 
     /**
      * @return the hash
      */
     public Long getHash() {
         return hash;
     }
 
     /**
      * @param arch the arch to set
      */
     public void setArch(String arch) {
         this.arch = arch;
     }
 
     /**
      * @return the arch
      */
     public String getArch() {
         return arch;
     }
 
     /**
      * @param version the version to set
      */
     public void setVersion(String version) {
         this.version = version;
     }
 
     /**
      * @return the version
      */
     public String getVersion() {
         return version;
     }
 
     /**
      * @param variant the variant to set
      */
     public void setVariant(String variant) {
         this.variant = variant;
     }
 
     /**
      * @return the variant
      */
     public String getVariant() {
         return variant;
     }
 
     public Set<Content> getContent() {
         return content;
     }
 
     public void setContent(Set<Content> content) {
         this.content = content;
     }
     
     public void addContent(Content content) {
         if (this.content != null) {
             this.content = new HashSet<Content>();
         }
         if (!this.content.contains(content)) { 
             this.content.add(content);
         }
     }
 
     /**
      * @param enabledContent the enabledContent to set
      */
     public void setEnabledContent(Set<Content> enabledContent) {
         this.enabledContent = enabledContent;
     }
     
     public void addEnabledContent(Content content) {
         if (this.enabledContent != null) {
             this.enabledContent = new HashSet<Content>();
         }
         if (!this.enabledContent.contains(content)) { 
             this.enabledContent.add(content);
         }
     }
 
     /**
      * @return the enabledContent
      */
     public Set<Content> getEnabledContent() {
 //        return enabledContent;
         return content;
     }
 
  
 }

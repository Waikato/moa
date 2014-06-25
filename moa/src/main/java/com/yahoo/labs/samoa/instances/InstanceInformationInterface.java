package com.yahoo.labs.samoa.instances;

import java.util.List;

public interface InstanceInformationInterface {

	/**
	 * Sets the dataset's name.
	 *
	 * @param string the new dataset's name
	 */
	public void setRelationName(String string);

	/**
	 * Gets the dataset's name.
	 *
	 * @return the dataset's name
	 */
	public String getRelationName();

	/**
	 * Class index.
	 *
	 * @return the int
	 */
	public int classIndex();

	/**
	 * Sets the class index.
	 *
	 * @param classIndex the new class index
	 */
	public void setClassIndex(int classIndex);

	/**
	 * Class attribute.
	 *
	 * @return the attribute
	 */
	public Attribute classAttribute();

	/**
	 * Num attributes.
	 *
	 * @return the int
	 */
	public int numAttributes();

	/**
	 * Attribute.
	 *
	 * @param w the w
	 * @return the attribute
	 */
	public Attribute attribute(int w);

	/**
	 * Num classes.
	 *
	 * @return the int
	 */
	public int numClasses();

	/**
	 * Delete attribute at.
	 *
	 * @param integer the integer
	 */
	public void deleteAttributeAt(Integer integer);

	/**
	 * Insert attribute at.
	 *
	 * @param attribute the attribute
	 * @param i the i
	 */
	public void insertAttributeAt(Attribute attribute, int i);

	/**
	 * Sets the attribute information.
	 *
	 * @param v the new attribute information
	 */
	public void setAttributes(List<Attribute> v);

}
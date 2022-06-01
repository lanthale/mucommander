package com.mucommander.commons.file.impl.vsphere;

import com.vmware.vim25.ManagedObjectReference;

/**
 * A wrapper for ManagedObjectReferenceWrapper that adds equals and hashCode for it.
 * 
 * @author Yuval Kohavi, yuval.kohavi@intigua.com
 *
 */
public class ManagedObjectReferenceWrapper {
	private final ManagedObjectReference mor;

	public ManagedObjectReferenceWrapper(ManagedObjectReference mor) {
		this.mor = mor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mor.getVal() == null) ? 0 : mor.getVal().hashCode());
		result = prime * result
				+ ((mor.getType() == null) ? 0 : mor.getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManagedObjectReferenceWrapper other = (ManagedObjectReferenceWrapper) obj;
		if (mor.getVal() == null) {
			if (other.mor.getVal() != null)
				return false;
		} else if (!mor.getVal().equals(other.mor.getVal()))
			return false;
		if (mor.getType() == null) {
            return other.mor.getType() == null;
		} else return mor.getType().equals(other.mor.getType());
    }

	@Override
	public String toString() {
		return "ManagedObjectReferenceWrapper [type=" + mor.getType() + ", value=" + mor.getVal() + "]";
	}

	/**
	 * @return the mor
	 */
	public ManagedObjectReference getMor() {
		return mor;
	}

}

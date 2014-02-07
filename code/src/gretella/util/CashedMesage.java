

package gretella.util;


public class CashedMesage {
    private byte[] GUID;
    private byte type;
    
    public CashedMesage( byte[] id , byte t ){
        this.GUID = id;
        this.type = t;
    }
    
    
    @Override
    public boolean equals( Object arg ) {
        if( arg.getClass().getName().equals( this.getClass().getName() ) == false )
            return false;
        CashedMesage obj = (CashedMesage)arg;
        
        if( GUID.equals( obj.getGUID() ) && type == obj.getType() )
            return true;
        else
            return false;
        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + ( this.GUID != null ? this.GUID.hashCode() : 0 );
        hash = 79 * hash + this.type;
        return hash;
    }

    public byte[] getGUID() {
        return GUID;
    }

    public void setGUID( byte[] GUID ) {
        this.GUID = GUID;
    }

    public byte getType() {
        return type;
    }

    public void setType( byte type ) {
        this.type = type;
    }
}

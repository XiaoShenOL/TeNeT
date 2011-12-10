//XXX author Ruixin Qiang
package tenet.protocol.datalink.sfdatalink;


import java.util.LinkedList;

import tenet.core.Simulator;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.physics.Link;

/**
* ���㽻����Ҫʹ�õ��ľ��д���ת��������IDatalinkLayer
* һ����˵��ֻ��L2Switch��ʹ�õ�StoreForwardingDatalink
* ͬʱ��StoreForwardingDatalink�󶨵���Nodeһ����L2Switch
*/
public class StoreForwardingDatalink extends SimpleEthernetDatalink {

	protected final static boolean debug=false;

    /**
     * ��Ҫת����֡�Ķ���
     */
	private LinkedList<FrameParamStruct> sendQueue;

	private boolean noFrameOnGoing  = true;
	
	public StoreForwardingDatalink(MediumAddress m_mac) {
		super(m_mac);
		sendQueue = new LinkedList<FrameParamStruct>();
	}

	/**
	* ����·�Ͽ�ʱ���еĴ������
	*/
	@Override
	protected void linkDown() {
		super.linkDown(); //���ø�����̣�ʹ��״̬�ı���źŴﵽ������ضԶ���
		//���������жϵĵȴ�״̬��������յ���������жϵ����߼�����
		this.resetInterrupt(Link.INT_LINK_READ_ERROR);
		this.resetInterrupt(Link.INT_LINK_READ_OK);
		this.resetInterrupt(INT_RECEIVE_LINKDOWN);
		this.resetInterrupt(INT_RECEIVE_COLLISION);
		this.resetInterrupt(Link.INT_LINK_SEND_ERROR);
		this.resetInterrupt(Link.INT_LINK_SEND_OK);
		this.resetInterrupt(INT_SEND_LINKDOWN);
		this.resetInterrupt(INT_SEND_COLLISION);
		this.sendQueue.clear();//���еȴ����͵�֡�����Ա�����
	}

	/**
	* ����·��Ϊ����״̬ʱ���еĴ���
	*/
	@Override
	protected void linkUp() {
		super.linkUp();//���ø�����̣�ʹ��״̬�ı���źŴﵽ������ضԶ���
		this.waitReceiveSignal();//�ȴ����н�����Ҫ���ж��ź�
	}

	//�������Щ���̶�����SimpleEthernetDatalink.interruptHandler�е��õĺ����������Ӧ���ж��źſ���ȥԭ�����鿴
	
	/**
	 * �������֡���ִ�������
	 */
	@Override
	protected void onReadError(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * �������֡��ȷ�����
	 */
	@Override
	protected void onReadOK(FrameParamStruct param) {
		if (m_node instanceof InterruptObject) {
			ReceiveParam frame =  new ReceiveParam(ReceiveStatus.receiveOK, (InterruptObject) m_node, this,
						(FrameParamStruct) param);
			((InterruptObject)m_node).delayInterrupt(INT_FRAME_RECEIVE, frame, m_delay);
		}
		this.waitReceiveSignal();
	}

	/**
	 * �������֡����У���������
	 */
	@Override
	protected void onReadOKwithCheckError(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * ������ֽ��ճ�ͻ�����жϵ����
	 */
	@Override
	protected void onReceiveRequireCollision(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * ������ַǽ���״̬���յ����жϵ����
	 */
	@Override
	protected void onReceiveRequireLinkDown(FrameParamStruct param) {
		this.waitReceiveSignal();
	}

	/**
	 * ������֡���ִ�������
	 */
	@Override
	protected void onSendError(FrameParamStruct param) {
		onSendOK(param);
	}

	/**
	 * ����ɹ�����֡�����
	 */
	@Override
	protected void onSendOK(FrameParamStruct param) {
		if (!isLinkUp()) 
			return;
		noFrameOnGoing = true;
		while (true) {
			if (sendQueue.isEmpty())
				break;
			FrameParamStruct next = sendQueue.remove();
			if (next != null && next.dataParam.length <= m_MTU) { 
				waitTransmitSignal();
				TransmitFrame command = new TransmitFrame(Simulator.getInstance().getTime() + m_delay, this, next.toBytes()); 
				Simulator.getInstance().schedule(command);
				noFrameOnGoing = false;
				break;
			}
		}
	}

	/**
	 * ������ַ��ͳ�ͻ�����жϵ����
	 */
	@Override
	protected void onTransmitRequireCollision(FrameParamStruct param) {
		onSendOK(param);
	}

	/**
	 * ������ַǽ���״̬���͵����жϵ����
	 */
	@Override
	protected void onTransmitRequireLinkDown(FrameParamStruct param) {
		onSendOK(param);
	}
	
	@Override 
	public void transmitFrame(FrameParamStruct frame) {
		sendQueue.add(frame);
		if (noFrameOnGoing) 
			onSendOK(null);
	}
}
